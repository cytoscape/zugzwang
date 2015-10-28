#version 450
//#extension GL_ARB_bindless_texture : require

uniform mat4 viewMatrix;

layout (location = 0) in vec3 in_position;
layout (location = 1) in uvec3 in_sizes;
//layout (location = 2) in uint in_texID;
// ADD FLAT QUALIFIER TO TEXTUREID!

out VS_OUT
{
	uvec3 sizes;
	flat int texID;
} vs_out;

void main(void)
{
	vs_out.sizes = in_sizes;
	vs_out.texID = gl_VertexID / 2;
	vec4 transformed = viewMatrix * vec4(in_position, 1.0f);
	gl_Position = transformed;
}