#version 310 es

//TODO do I want this in place? do I want this to be just a mask?

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D frameBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

layout(shared, binding = 0) coherent buffer FingerPosData {
    float buff[];
} fingerPos;


layout (local_size_x = 8, local_size_y = 8) in;

struct FingerCandidate {
    int x;
    int y;
    float thresh;
    uint sem;
};

//todo make an array of struct consisting of an x,y pos and a accum value
 // todo might need to add more stuff later but good starting point

layout(shared, binding = 2) coherent buffer FingerCandidates {
    FingerCandidate buff[];
} fingCand;



uniform int fingerAccumThresh;

void drawBox5x5(in ivec2 globalPos, in vec4 colour) {
    imageStore(frameBuffer, globalPos + ivec2(-2,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2(-2,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-2, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2(-2, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-2, 2), colour);

    imageStore(frameBuffer, globalPos + ivec2(-1,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 2), colour);

    imageStore(frameBuffer, globalPos + ivec2( 0,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 2), colour);

    imageStore(frameBuffer, globalPos + ivec2( 1,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 2), colour);

    imageStore(frameBuffer, globalPos + ivec2( 2,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2( 2,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 2, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 2, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 2, 2), colour);
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    /*
    for (int i = 0; i<5; ++i) {
        if (pos == ivec2(fingCand.buff[i].x,fingCand.buff[i].y)) {
            drawBox5x5(pos, vec4(1,1,1,1));
            //imageStore(frameBuffer, pos, vec4(1));
        }
        // TODO foul black magic afoot why does this compile with above (drawBox inc.) but not without above
        if (abs(pos.x - fingCand.buff[i].x) < 10 && abs(pos.y - fingCand.buff[i].y) < 10) {
            imageStore(frameBuffer, pos, vec4(1));
        }
    }*/
    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y);

    // TODO if I keep this find max thing I should change to SSBO
    float rat = fingerPos.buff[offset] / float(fingCand.buff[0].sem);

    imageStore(frameBuffer, pos, vec4(vec3(1.0) * rat, 1.0));
}
