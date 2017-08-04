#version 310 es

#extension  GL_NV_shader_atomic_float : require

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;

struct Moments {
    float m00;
    float m01;
    float m10;
    float m11;
    float m02;
    float m20;
    float cogX;
    float cogY;
    float mu20Prime;
    float mu02Prime;
    float mu11Prime;
    float angle;
    float slope;
    float yIntercept;
    float perpSlope;
    float perpYIntercept;
};


layout(shared, binding = 0) coherent buffer MomentBuffer {
    Moments data;
} moments;

layout (local_size_x = 8, local_size_y = 8) in;

#define PI 3.14159265359
#define HALF_PI 1.57079632679

uniform float fillInHolesThresh;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 pix = imageLoad(framebuffer, pos);

    if (pix.a == 0.0) {
        float perpYIntercept = float(pos.y) - (moments.data.perpSlope * float(pos.x));

        // search for surrounding points which are non zero perpendicular to arm direction line
        int dist = 0;
        bool foundNonZeroAbove = false;
        bool foundNonZeroBelow = false;
        while (dist < int(fillInHolesThresh)) {
            if (!foundNonZeroAbove) {
                vec4 pixAbove = imageLoad(framebuffer, ivec2(pos.x - dist, moments.data.perpSlope * float(pos.x - dist) + perpYIntercept));
                if (pixAbove.a != 0.0) foundNonZeroAbove = true;
            }
            if (!foundNonZeroBelow) {
                vec4 pixBelow = imageLoad(framebuffer, ivec2(pos.x + dist, moments.data.perpSlope * float(pos.x + dist) + perpYIntercept));
                if (pixBelow.a != 0.0) foundNonZeroBelow = true;
            }
            dist += 1;
        }

        if (foundNonZeroBelow && foundNonZeroAbove) {
            imageStore(framebuffer, pos, vec4(1.0));
        }
    }
}
