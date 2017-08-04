#version 310 es

layout (local_size_x = 8, local_size_y = 8) in;

layout(shared, binding = 7) coherent buffer AngleDebugBuff {
    int index;
    float buff[];
} angleDebugBuff;

void main() {
   ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

   uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
   uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;
   uint offset = uint(pos.x * int(height) + pos.y);

   angleDebugBuff.index = 0;
   angleDebugBuff.buff[offset] = 0.0;
}
