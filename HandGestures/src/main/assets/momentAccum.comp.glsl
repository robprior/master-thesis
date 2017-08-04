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

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 pix = imageLoad(framebuffer, pos);

    // from previous step all non foreground pixels will be set to 0
    if(pix.a != 0.0) {
        // moments follow form sum I(x,y)*x^p*y^q where I(x,y) is a binary image
        atomicAdd(moments.data.m00, 1.0);
        atomicAdd(moments.data.m01, float(pos.y));
        atomicAdd(moments.data.m10, float(pos.x));
        //atomicAdd(moments.data.m11, float(pos.x) * float(pos.y));
        //atomicAdd(moments.data.m02, float(pos.y) * float(pos.y));
        //atomicAdd(moments.data.m20, float(pos.x) * float(pos.x));
    }
}
