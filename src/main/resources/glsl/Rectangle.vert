#version 440
//#extension GL_ARB_bindless_texture : require

uniform mat4 viewMatrix;

layout (location = 0) in vec3 in_position;
layout (location = 1) in ivec4 in_sizes;
layout (location = 2) in ivec2 in_offset;
// ADD FLAT QUALIFIER TO TEXTUREID!

out VS_OUT
{
	ivec4 sizes;
	ivec2 offset;
	flat int texID;
} vs_out;

void main(void)
{
	vs_out.sizes = in_sizes;
	vs_out.offset = in_offset;
	vs_out.texID = gl_VertexID;
	vec4 transformed = viewMatrix * vec4(in_position, 1.0f);
	//transformed.z -= 1e-2f + 1e-2f * float(gl_VertexID);
	gl_Position = transformed;
}