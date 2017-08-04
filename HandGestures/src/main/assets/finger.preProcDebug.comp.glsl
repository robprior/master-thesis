#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

// this does nothing but want this shader to be interchangable with non debug version
uniform bool removeContourBreaks;

const ivec2[] DISPLACEMENTS = ivec2[8](
    ivec2(-1, -1),
    ivec2( 0, -1),
    ivec2( 1, -1),
    ivec2(-1,  0),

    ivec2( 1,  0),
    ivec2(-1,  1),
    ivec2( 0,  1),
    ivec2( 1,  1)
);

void drawBox(in ivec2 globalPos, in vec4 colour) {
    imageStore(outputBuffer, globalPos + ivec2(-1,-1), colour);
    imageStore(outputBuffer, globalPos + ivec2(-1, 0), colour);
    imageStore(outputBuffer, globalPos + ivec2(-1, 1), colour);
    imageStore(outputBuffer, globalPos + ivec2( 0,-1), colour);
    imageStore(outputBuffer, globalPos + ivec2( 0, 0), colour);
    imageStore(outputBuffer, globalPos + ivec2( 0, 1), colour);
    imageStore(outputBuffer, globalPos + ivec2( 1,-1), colour);
    imageStore(outputBuffer, globalPos + ivec2( 1, 0), colour);
    imageStore(outputBuffer, globalPos + ivec2( 1, 1), colour);
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 c = imageLoad(inputBuffer, pos);
    vec4 neighbourPos;

    if (c.a != 0.0) {
        vec4 outputEncoding;
        int offset = 0, intensity = 0;
        for (int i= 0; i<8; ++i) {
            ivec2 candidate = DISPLACEMENTS[i] + pos;
            vec4 neighbour = imageLoad(inputBuffer, candidate);
            if (any(notEqual(neighbour, vec4(0.0)))) {
                if (offset<4) {
                    outputEncoding[0 + offset] = float(DISPLACEMENTS[i].x);
                    outputEncoding[1 + offset] = float(DISPLACEMENTS[i].y);
                    offset +=2;
                }
                intensity++;
            }
        }
        if (intensity <2) {
            drawBox(pos, vec4(1.0, 1.0, 0.0, 1.0)); // yellow
        } else if (intensity == 2) {
            imageStore(outputBuffer, pos, outputEncoding);
        } else {
            drawBox(pos, vec4(0.0, 1.0, 1.0, 1.0)); // cyan
        }
    }
}
