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

    if (pos == ivec2(0,0)) {
        //TODO why is this the only loop out of all the code I've written that needs to be manually unrolled
        /*
        for (int i = 0; i<5; ++i) {
            fingCand.buff[0].x = 0;
            fingCand.buff[0].y = 0;
            fingCand.buff[0].thresh = 0.0f;
            fingCand.buff[0].sem = 0u;
        }*/

        //  fingCand.buff[0].x = 0;
        //  fingCand.buff[0].y = 0;
            fingCand.buff[0].thresh = 0.0f;
            fingCand.buff[0].sem = 0u;

        //  fingCand.buff[1].x = 0;
        //  fingCand.buff[1].y = 0;
            fingCand.buff[1].thresh = 0.0f;
            fingCand.buff[1].sem = 0u;

        //  fingCand.buff[2].x = 0;
        //  fingCand.buff[2].y = 0;
            fingCand.buff[2].thresh = 0.0f;
            fingCand.buff[2].sem = 0u;

        //  fingCand.buff[3].x = 0;
        //  fingCand.buff[3].y = 0;
            fingCand.buff[3].thresh = 0.0f;
            fingCand.buff[3].sem = 0u;

        //  fingCand.buff[4].x = 0;
        //  fingCand.buff[4].y = 0;
            fingCand.buff[4].thresh = 0.0f;
            fingCand.buff[4].sem = 0u;
    }
}
