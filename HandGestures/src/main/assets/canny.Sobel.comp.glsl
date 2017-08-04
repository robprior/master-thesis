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
    vec4 alpha = imageLoad(inputBuffer, pos);

    vec4 gradIntensity = vec4(0.0);

    if (alpha.a != 0.0) {
        vec4 tl = imageLoad(inputBuffer, pos + ivec2(-1,-1));
        vec4 t  = imageLoad(inputBuffer, pos + ivec2( 0,-1));
        vec4 tr = imageLoad(inputBuffer, pos + ivec2( 1,-1));
        vec4 r  = imageLoad(inputBuffer, pos + ivec2(-1, 0));
        vec4 l  = imageLoad(inputBuffer, pos + ivec2( 1, 0));
        vec4 bl = imageLoad(inputBuffer, pos + ivec2(-1, 1));
        vec4 b  = imageLoad(inputBuffer, pos + ivec2( 0, 1));
        vec4 br = imageLoad(inputBuffer, pos + ivec2( 1, 1));
        /* -1 0 1
           -2 0 2
           -1 0 1
        */
        float gx = (-1.0*tl.r) + (    tr.r) +
                   (-2.0* l.r) + (2.0* r.r) +
                   (-1.0*bl.r) + (    br.r);
        /* -1 -2 -1
            0  0  0
            1  2  1
        */
        float gy = (-1.0*tl.r) + (-2.0* t.r) + (-1.0*tr.r) +
                   ( 1.0*bl.r) + (-2.0* b.r) + (1.0*br.r);

        gradIntensity =  vec4(gx,gy,sqrt(gx*gx+gy*gy),1.0);
    }
    imageStore(outputBuffer, pos, gradIntensity);
}
