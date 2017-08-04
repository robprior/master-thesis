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

        // on pass 2 there are 4 distinct nieghbourhood checks, if any are true the pixel is deleted
        int test = 0;
        test += int(p1.a == 1.0 && p8.a == 1.0 && p6.a == 1.0 && p3.a == 0.0);
        test += int(p3.a == 1.0 && p4.a == 1.0 && p6.a == 1.0 && p1.a == 0.0);
        test += int(p5.a == 1.0 && p6.a == 1.0 && p8.a == 1.0 && p3.a == 0.0);
        test += int(p4.a == 1.0 && p6.a == 1.0 && p7.a == 1.0 && p1.a == 0.0);
        // these are not part of the test listed in the paper BAD causes mass amounts of non continuous
        //test += int(p8.a == 1.0 && p2.a == 1.0 && p3.a == 1.0 && p5.a == 0.0);
        //test += int(p4.a == 1.0 && p2.a == 1.0 && p1.a == 1.0 && p7.a == 0.0);
        //test += int(p7.a == 1.0 && p8.a == 1.0 && p2.a == 1.0 && p5.a == 0.0);
        //test += int(p5.a == 1.0 && p4.a == 1.0 && p2.a == 1.0 && p7.a == 0.0);

        // for a pixel to be deleted / considered redundant it must pass at least 1 of the above
        if (test > 0) {
            imageStore(outputBuffer, pos, vec4(0.0));
        }
    }

}
