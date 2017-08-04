#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

uniform int iteration;

// implementation of zhang suen algorithm https://rosettacode.org/wiki/Zhang-Suen_thinning_algorithm
void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 c = imageLoad(inputBuffer, pos);

    if (c.a != 0.0) {
            // naming convention seen in literature it is clockwise around point starting at top left
            vec4 p1 = imageLoad(inputBuffer, pos + ivec2(-1, 1));
            vec4 p2 = imageLoad(inputBuffer, pos + ivec2( 0, 1));
            vec4 p3 = imageLoad(inputBuffer, pos + ivec2( 1, 1));
            vec4 p4 = imageLoad(inputBuffer, pos + ivec2( 1, 0));
            vec4 p5 = imageLoad(inputBuffer, pos + ivec2( 1,-1));
            vec4 p6 = imageLoad(inputBuffer, pos + ivec2( 0,-1));
            vec4 p7 = imageLoad(inputBuffer, pos + ivec2(-1,-1));
            vec4 p8 = imageLoad(inputBuffer, pos + ivec2(-1, 0));

            // need to find all transitions from black to white from top clockwise around the center
            int AP = int(p2.a == 0.0 && p3.a == 1.0) + int(p3.a  == 0.0 &&  p4.a == 1.0) +
                     int(p4.a == 0.0 && p5.a == 1.0) + int(p5.a  == 0.0 &&  p6.a == 1.0) +
                     int(p6.a == 0.0 && p7.a == 1.0) + int(p7.a  == 0.0 &&  p8.a == 1.0) +
                     int(p8.a == 0.0 && p1.a == 1.0) + int(p1.a  == 0.0 &&  p2.a == 1.0);

            // again naming convention from literature BP is non zero neighbours
            int BP = int(p1.a + p2.a + p3.a + p4.a + p5.a + p6.a + p7.a + p8.a);

            // then we check specific neighbours if they any are 0
            int neighCheck1 = iteration == 0 ? int(p2.a * p4.a * p6.a) : int(p2.a * p4.a * p8.a);
            int neighCheck2 = iteration == 0 ? int(p4.a * p6.a * p8.a) : int(p2.a * p6.a * p8.a);

            // for a pixel to be deleted it must satisfy all criterion
            if (AP == 1 && (2 <=BP && BP <=6) && neighCheck1 == 0 && neighCheck2 == 0) {
                imageStore(outputBuffer, pos, vec4(0.0));
            }
    }
}
