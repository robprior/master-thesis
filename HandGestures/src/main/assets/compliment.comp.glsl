#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 inputPix = imageLoad(inputBuffer, pos);
    vec4 outColour = vec4(1.0);

    if (inputPix.a != 0.0) {
       outColour = vec4(0.0);
    }
    imageStore(outputBuffer, pos, outColour);
}
