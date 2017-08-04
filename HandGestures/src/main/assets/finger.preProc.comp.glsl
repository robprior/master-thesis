#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

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

const vec4 MANY_NEIGHBOUR = vec4(-2.0);

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 c = imageLoad(inputBuffer, pos);

    if (c.a != 0.0) {
        vec4 outputEncoding;
        int offset = 0;
        for (int i= 0; i<8; ++i) {
            ivec2 candidate = DISPLACEMENTS[i] + pos;
            vec4 neighbour = imageLoad(inputBuffer, candidate);
            if (any(notEqual(neighbour, vec4(0.0)))) {
                if(offset < 4) {
                    outputEncoding[0 + offset] = float(DISPLACEMENTS[i].x);
                    outputEncoding[1 + offset] = float(DISPLACEMENTS[i].y);
                }
                offset +=2;
            }
            if(offset >= 4) break;
        }
        imageStore(outputBuffer, pos, outputEncoding);

        if(offset > 4 && removeContourBreaks) {
            imageStore(outputBuffer, pos, outputEncoding);
        }
    }
}
