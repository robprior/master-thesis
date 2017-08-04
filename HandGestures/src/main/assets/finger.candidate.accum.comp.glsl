#version 310 es
#extension  GL_NV_shader_atomic_float : require

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D frameBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

layout(shared, binding = 0) coherent buffer FingerPosData {
    float buff[];
} fingerPos;

uniform int   neighbourhoodSize;
uniform float angleThreshMax;
uniform float angleThreshMin;
uniform bool  drawShortLine;
uniform bool  drawLongLine;

void findNextNeighbour(in ivec2 globalPos, inout vec2 lastPos, inout vec2 compoundDir, inout bool errorConHole, inout bool errorConLoop, inout bool errorManyNeighbours) {
    // load the next pixel which contains neighbourhood information for that pixel
    vec4 nextNeighbour = imageLoad(frameBuffer, globalPos + ivec2(int(compoundDir.x), int(compoundDir.y)));
    // need to check if next position is stored in xy (idicies 0,1) or zw (2,3)
    // try xy and see if we go backwards
    vec2 canditate = nextNeighbour.xy + compoundDir;
    // if we did go backwards use zw
    int idx = all(equal(lastPos, canditate)) ? 2 : 0;
    lastPos = compoundDir;
    compoundDir += vec2(nextNeighbour[0 + idx], nextNeighbour[1 + idx]);
    // are we back at original position ?
    errorConLoop = errorConLoop || all(equal(compoundDir, vec2(0)));
    // was there a hole in the contour?
    errorConHole = errorConHole || all(equal(nextNeighbour, vec4(0.0)));
    // is the point a junction on the contour TODO kinda hacky magic number
    errorManyNeighbours = errorManyNeighbours || all(equal(nextNeighbour, vec4(-2.0)));
}

// don't need anything fance just using this: https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
// implementations shown there only works in 1st octant (x,y > 0 && x>y)
// note this isn't general purpose as drawing line relative to starting point
void drawLine(in ivec2 globalPos, in vec2 endPos, in uint height) {
    // this divides quadrants to octants, not caring about which quadrant yet
    ivec2 absEndPos = ivec2(abs(int(endPos.x)), abs(int(endPos.y)));
    // always are going to be drawing globalPos + x,y offset but will be changing x,y differently
    // based on octet so want to index into a vector
    ivec2 itrs = ivec2(0);
    // same with ops depending on quadrant
    // so for example x < 0 && y > 0 && abs(x)>abs(y) (octant 4) want to go form 0,-1,..->x and from 0,1,..y
    ivec2 ops  = ivec2(0);

    // all of the above change based on if x > y
    int bigDirIdx, smallDirIdx;

    // if it is we are in octants 1,4,5,8
        // so the x index needs to change every loop iteration while the y changes occasionally
        // how x changes depends on quadrant
    // else y changes each iteration
    if (absEndPos.x >= absEndPos.y) {
        bigDirIdx   = 0;
        smallDirIdx = 1;
    } else {
        bigDirIdx   = 1;
        smallDirIdx = 0;
    }

    int eps = 0;
    // this is where quadrant is encoded we are going from 0 + ops to end pos in the loop
    // op will either be +/- 1 depending on quadrant
    ops[bigDirIdx]   = (endPos[bigDirIdx]   >= 0.0) ? 1 : -1;
    ops[smallDirIdx] = (endPos[smallDirIdx] >= 0.0) ? 1 : -1;

    // while the index we change every iteration is less than the end point
    while (abs(itrs[bigDirIdx]) < absEndPos[bigDirIdx]) {
        // store the current position (ignoring the big/small index stuff to ensure x,y position is correct)
        //TODO
        ivec2 pos = globalPos + itrs;
        atomicAdd(fingerPos.buff[pos.x * int(height) + pos.y], 1.0);
        // add to the error based on the small distance
        eps += absEndPos[smallDirIdx];
        // if the error becomes larger than the big change then we need to change the small itr
        // Example drawing line from 0,0 to 4,1 (bigDirection is x, small y)
            // 1st iter draw pixel at 0,0 increase error to 1, 2*1 < 4 so don't change y
            // 2nd iter draw pixel at 1,0 increase error to 2, 2*2>=4 increase y and reduce error to -2
            // 3rd iter draw pixel at 2,1 increase error to -1
            // 4th iter draw pixel at 3,1 increase error to 0
            // 5th iter draw pixel at 4,1 increase error to 1 and we're done
        if ( 2 * eps >= absEndPos[bigDirIdx]) {
            // again the actual changes depend on the quadrant
            itrs[smallDirIdx] += ops[smallDirIdx];
            eps -= absEndPos[bigDirIdx];
        }
        itrs[bigDirIdx] += ops[bigDirIdx];
    }
}


void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y);

    vec4 c = imageLoad(frameBuffer, pos);

    if (any(notEqual(c, vec4(0.0)))) {
        // after pre preocessing every pixel contains locations of left / right neighbours
        vec2 leftCmp = c.xy;
        vec2 rghtCmp = c.zw;
        vec2 lastLeftPos = vec2(0,0);
        vec2 lastRghtPos = vec2(0,0);
        bool errorHole = false;
        bool errorLoop = false;
        bool errorManyNeighbours = false;

        for (int i = 0; i < neighbourhoodSize; ++i) {
            findNextNeighbour(pos, lastLeftPos, leftCmp, errorHole, errorLoop, errorManyNeighbours);
            findNextNeighbour(pos, lastRghtPos, rghtCmp, errorHole, errorLoop, errorManyNeighbours);
            if (errorHole || errorLoop || errorManyNeighbours) {
                break;
            }
        }
        // leftCmp and rightCmp now contain Pi+k and Pi-k
        float angle = acos(dot(leftCmp, rghtCmp)/(length(leftCmp) * length(rghtCmp)));
        float angleDegrees = degrees(angle);
        if (!errorManyNeighbours && angleThreshMin < angleDegrees && angleDegrees < angleThreshMax) {
            if (length(leftCmp) < length(rghtCmp)) {
                drawLine(pos, leftCmp, height);
            } else {
                drawLine(pos, rghtCmp, height);
            }
        }
    }
}
