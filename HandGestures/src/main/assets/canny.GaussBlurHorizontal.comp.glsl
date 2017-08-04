#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

// TODO add in a parameter for stddev

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    //uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    //uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    //uint offset = uint(pos.x * int(height) + pos.y);

    vec4 accum = vec4(0.0);
    accum += 1.0 * imageLoad(inputBuffer, pos + ivec2(-2,0));
    accum += 4.0 * imageLoad(inputBuffer, pos + ivec2(-1,0));
    accum += 6.0 * imageLoad(inputBuffer, pos + ivec2( 0,0));
    accum += 4.0 * imageLoad(inputBuffer, pos + ivec2( 1,0));
    accum += 1.0 * imageLoad(inputBuffer, pos + ivec2( 2,0));
    accum /= 16.0;

    imageStore(outputBuffer, pos, accum);
}
