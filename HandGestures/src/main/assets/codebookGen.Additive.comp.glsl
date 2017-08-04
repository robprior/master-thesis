#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;


struct CodeElement {
    float Ihigh;
    float Ilow;
    float min;
    float max;
    float tLast;
    float stale;
};

uniform uint numCE;

layout(shared, binding = 0) coherent buffer CodeBookBuffer {
    CodeElement data[];
} codeBook;


layout (local_size_x = 8, local_size_y = 8) in;

//#define BOUNDS 0.2
//#define BOUNDS 0.2
//#define I_INC 0.05
uniform float BOUNDS;
uniform float I_INC;

void updateCodeElement(uint offset, float I) {
    codeBook.data[offset].tLast += 1.0;
    if (codeBook.data[offset].min > I) {
        codeBook.data[offset].min = I;
    }
    if (codeBook.data[offset].max < I) {
        codeBook.data[offset].max = I;
    }
    //TODO
    if (codeBook.data[offset].Ihigh > I + BOUNDS) {
        codeBook.data[offset].Ihigh += I_INC;
    }
    if (codeBook.data[offset].Ilow < I - BOUNDS) {
        codeBook.data[offset].Ilow -= I_INC;
    }
}

void createCodeElement(uint offset, float I) {
    codeBook.data[offset].tLast = 1.0;
    codeBook.data[offset].min = I;
    codeBook.data[offset].max = I;
    codeBook.data[offset].Ihigh = I + I_INC;
    codeBook.data[offset].Ilow  = I - I_INC;
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y) * uint(numCE);

    vec4 pix = imageLoad(framebuffer, pos);

    //TODO try to optimie later
    uint i;
    float minTlast = 100000000.0f; //TODO max f
    uint minIdx = numCE;
    float I = pix.r + pix.g + pix.b;
    bool updatedCE = false;
    for (i = uint(0); i<numCE; ++i) {
        if (codeBook.data[offset+i].Ilow < I && I <codeBook.data[offset+i].Ihigh) {
            updateCodeElement(offset + i, I);
            updatedCE = true;
            break;
        }

        // check if codebook still has empty entries if no get rid of the least used one
        if (codeBook.data[offset+i].tLast == 0.0) {
            minIdx = i;
            break;
        } else if (codeBook.data[offset+i].tLast  < minTlast) {
            minTlast = codeBook.data[offset+i].tLast;
            minIdx = i;
        }
    }
    // No CodeElement matched need to add a new one
    if(!updatedCE) {
        createCodeElement(offset+minIdx, I);
    }
}
