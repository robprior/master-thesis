#version 310 es
#extension  GL_NV_shader_atomic_float : require

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

void drawBox(in ivec2 globalPos, in vec4 colour) {
    imageStore(frameBuffer, globalPos + ivec2(-1,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 1), colour);
}


void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y);


    /*
    if (fingerPos.buff[offset] > float(fingerAccumThresh)) {
        //TODO I think something like only allow updates if it is close by or if it is zero will work well
        //TODO use dist thresh for "close by"ness
        bool replacedMax = false;
        for (int i = 0; i<5; ++i) {
            float dist = sqrt((pos.x - fingCand.buff[i].x) * (pos.x - fingCand.buff[i].x) +
                              (pos.y - fingCand.buff[i].y) * (pos.y - fingCand.buff[i].y));
            bool written = false;
            //if (dist < minDist) continue;
            do {
                bool canWrite = atomicCompSwap(fingCand.buff[i].sem, 0u, 1u) == 0u;
                if (canWrite) {
                    memoryBarrier();
                    if (fingerPos.buff[offset] > fingCand.buff[i].thresh) {
                        fingCand.buff[i].thresh = fingerPos.buff[offset];
                        fingCand.buff[i].x = pos.x;
                        fingCand.buff[i].y = pos.y;
                        replacedMax = true;
                    }
                    written = true;
                    atomicExchange(fingCand.buff[i].sem, 0u);
                }
            } while (!written);
            if (replacedMax) break;
        }
    }*/
    //TODO if keep this find max thing I should change how the SSBO is laid out rather than this reuse
    atomicMax(fingCand.buff[0].sem, uint(fingerPos.buff[offset]));
}
