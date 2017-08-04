#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    //uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    //uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    //uint offset = uint(pos.x * int(height) + pos.y);

    vec4 pix   = imageLoad(inputBuffer, pos);

    imageStore(outputBuffer, pos, vec4(vec3(0.2126 * pix.r + 0.7152 * pix.g + 0.0722 * pix.b),pix.a));
}
