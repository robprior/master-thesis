#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D inputBuffer;

layout(binding = 3, rgba32f) uniform  image2D outputBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 c = imageLoad(inputBuffer, pos);

    vec4 gradIntensity = vec4(0.0);

    if (c.a != 0.0) {
        float intensity = 0.0;

        vec4 bl = imageLoad(inputBuffer, pos + ivec2(-1,-1));
        vec4 b  = imageLoad(inputBuffer, pos + ivec2( 0,-1));
        vec4 br = imageLoad(inputBuffer, pos + ivec2( 1,-1));
        vec4 l  = imageLoad(inputBuffer, pos + ivec2(-1, 0));
        vec4 r  = imageLoad(inputBuffer, pos + ivec2( 1, 0));
        vec4 tl = imageLoad(inputBuffer, pos + ivec2(-1, 1));
        vec4 t  = imageLoad(inputBuffer, pos + ivec2( 0, 1));
        vec4 tr = imageLoad(inputBuffer, pos + ivec2( 1, 1));
        intensity += tl.a;
        intensity +=  t.a;
        intensity += tr.a;
        intensity +=  r.a;
        intensity +=  l.a;
        intensity += bl.a;
        intensity +=  b.a;
        intensity += br.a;

        intensity = (intensity >= 8.0) ? 0.0 : intensity;
        intensity = (intensity <= 3.0) ? 0.0 : intensity;

        intensity = clamp(intensity,0.0,1.0);

        gradIntensity =  vec4(intensity);
    }
    imageStore(outputBuffer, pos, gradIntensity);
}
