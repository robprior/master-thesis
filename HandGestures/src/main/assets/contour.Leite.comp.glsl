#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D outputBuffer;

layout(binding = 3, rgba32f) uniform  image2D dilatedInput;
layout(binding = 4, rgba32f) uniform  image2D complimentInput;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 dilatedPix    = imageLoad(dilatedInput, pos);
    vec4 complimentPix = imageLoad(complimentInput, pos);
    vec4 outColour = vec4(0.0);

    if (dilatedPix.a != 0.0 && complimentPix.a != 0.0) {
       outColour = vec4(1.0);
    }
    imageStore(outputBuffer, pos, outColour);
}
