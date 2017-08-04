#version 310 es
#extension  GL_NV_shader_atomic_float : require

layout (local_size_x = 8, local_size_y = 8) in;

layout(shared, binding = 0) coherent buffer KMeansLabels {
    float labels[];
} kmeanLabel;

layout(shared, binding = 4) coherent buffer KMeansWeights {
    float weights[];
} kmeanWeights;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;
    uint offset = uint(pos.x * int(height) + pos.y);

    kmeanLabel.labels[offset]    = -1.0;
    kmeanWeights.weights[offset] =  0.0;
}
