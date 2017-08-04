#version 310 es
#extension  GL_NV_shader_atomic_float : require

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;

struct Moments {
    float m00;
    float m01;
    float m10;
    //float m11;
    //float m02;
    //float m20;
    float cogX;
    float cogY;
    float lastX;
    float lastY;
    //float mu20Prime;
    //float mu02Prime;
    //float mu11Prime;
    //float angle;
    //float slope;
    //float yIntercept;
    //float perpSlope;
    //float perpYIntercept;
};


layout(shared, binding = 0) coherent buffer MomentBuffer {
    Moments data;
} moments;

layout (local_size_x = 8, local_size_y = 8) in;

uniform float distThresh;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    int distX = pos.x - int(moments.data.cogX);
    int distY = pos.y - int(moments.data.cogY);

    // adding the read and first comparison is marginally faster
    vec4 pix = imageLoad(framebuffer, pos);
    if (pix.a != 0.0 && sqrt( (distY * distY) + (distX * distX) ) > distThresh) {
        imageStore(framebuffer, pos, vec4(0.0));
    }
}
