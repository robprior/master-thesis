#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;


struct CodeElement {
    float Ihigh;
    float Ilow;
    float min;
    float max;
    float tLast;
    float stale;
};

uniform uint numCE;

layout(shared, binding = 0) coherent buffer CodeBookBuffer {
    CodeElement data[];
} codeBook;

uniform float MAX_MOD;
uniform float MIN_MOD;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y) * uint(numCE);

    vec4 pix = imageLoad(framebuffer, pos);

    //TODO why is this not done already by an earlier step
    vec4 cameraColor = imageLoad(framebuffer, pos);

    // conv RGB in range 0-1 to YCbCr range Y:16-235 CbCr:16-240
    float y  =  16.0 + (cameraColor.r * 65.535 + cameraColor.g * 128.52  + cameraColor.b *  24.99);
    float cb = 128.0 + (cameraColor.r * -37.74 + cameraColor.g * -47.205 + cameraColor.b * 111.945);
    float cr = 128.0 + (cameraColor.r *111.945 + cameraColor.g * -93.84  + cameraColor.b * -18.105);

    // TODO can optimize this remove the divisions by changing factors above so YCbCr ranges from 0-1

     y =  y/235.0;
    cb = cb/240.0;
    cr = cr/240.0;
    //float y = cameraColor.r; float cb = cameraColor.g; float cr = cameraColor.b;

    float I = sqrt(y*y + cr*cr + cb *cb);

    uint i;
    bool isBackground = false;
    for (i = uint(0); i<numCE; ++i) {
        if (I < codeBook.data[offset+i].max + MAX_MOD && I > codeBook.data[offset+i].min - MIN_MOD ) {
            isBackground = true;
            break;
        }
    }

    if (isBackground) {
        imageStore(framebuffer, pos, vec4(0.0));
    }
    else {
        imageStore(framebuffer, pos, vec4(1.0));
    }
}
