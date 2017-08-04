#version 310 es


precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

uniform float magThresh;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    //uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    //uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    //uint offset = uint(pos.x * int(height) + pos.y);

    vec4 pix  = imageLoad(inputBuffer, pos);
    vec4 forwardCmp = vec4(0.0);
    vec4 backwrdCmp = vec4(0.0);
    float dx  = pix.r;
    float dy  = pix.g;
    float mag = pix.b;

    // not using atan2 here as no need to distinguish from 90 / 270 etc. just need 4 directions
    float dirA = atan(dy / dx);
    float absDir = abs(dirA);
    // need to round to nearest direction
    // n/s, ne/sw, nw/se, e/w -> 45 degrees 0.785398 rads per case
    //atan is between -pi/2 and pi/2
    //  e/w ->     0 +/- 0.39296991 (half of 45 deg)
    if (absDir < 0.39296991) {
        forwardCmp = imageLoad(inputBuffer, pos + ivec2( 1, 0));
        backwrdCmp = imageLoad(inputBuffer, pos + ivec2(-1, 0));
    //  n/s ->/ pi/2 +/- 0.39296991
    } else if (absDir > 1.17783009) {
        forwardCmp = imageLoad(inputBuffer, pos + ivec2( 0, 1));
        backwrdCmp = imageLoad(inputBuffer, pos + ivec2( 0,-1));
    // ne/sw -> else positive
    } else if (dirA > 0.0) {
        forwardCmp = imageLoad(inputBuffer, pos + ivec2( 1, 1));
        backwrdCmp = imageLoad(inputBuffer, pos + ivec2(-1,-1));
    // nw/se -> else negative
    } else {
        forwardCmp = imageLoad(inputBuffer, pos + ivec2( 1,-1));
        backwrdCmp = imageLoad(inputBuffer, pos + ivec2(-1, 1));
    }

    //TODO possible need to add a ignore based on mask here
    float outMag = 0.0;
    if (mag > forwardCmp.b && mag > backwrdCmp.b && mag > magThresh) outMag = 1.0;
    imageStore(outputBuffer, pos, vec4(vec3(outMag),pix.a));
}
