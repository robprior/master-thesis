#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

// implementation of zhang suen algorithm https://rosettacode.org/wiki/Zhang-Suen_thinning_algorithm
void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 pi = imageLoad(inputBuffer, pos);

    // we check for deletion of a pixel so if already 0 no need
    if (pi.a != 0.0) {
        // naming convention seen in literature it is clockwise around point starting at top left
        vec4 p1 = imageLoad(inputBuffer, pos + ivec2(-1, 1));
        vec4 p2 = imageLoad(inputBuffer, pos + ivec2( 0, 1));
        vec4 p3 = imageLoad(inputBuffer, pos + ivec2( 1, 1));
        vec4 p4 = imageLoad(inputBuffer, pos + ivec2( 1, 0));
        vec4 p5 = imageLoad(inputBuffer, pos + ivec2( 1,-1));
        vec4 p6 = imageLoad(inputBuffer, pos + ivec2( 0,-1));
        vec4 p7 = imageLoad(inputBuffer, pos + ivec2(-1,-1));
        vec4 p8 = imageLoad(inputBuffer, pos + ivec2(-1, 0));

        int BP = int(p1.a + p2.a + p3.a + p4.a + p5.a + p6.a + p7.a + p8.a);

        if (BP !=2) {
            imageStore(outputBuffer, pos, vec4(0.0));
        }
    }

}
