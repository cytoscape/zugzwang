#version 450
//#extension GL_ARB_bindless_texture : require

uniform mat4 projMatrix;

layout (lines) in;
layout (triangle_strip, max_vertices = 4) out;

in VS_OUT
{
	uvec3 sizes;
	flat int texID;
} gs_in[];

out GS_OUT
{
	vec2 texCoords;
	flat int texID;
} gs_out;

void main(void)
{	
	int texID = gs_in[0].texID;
	float halfWidth = float(gs_in[0].sizes.x) * 0.5f;
	float halfPixelU = 0.5f / float(gs_in[0].sizes.y);
	float halfPixelV = 0.5f / float(gs_in[0].sizes.z);
	
	vec2 tangent = gl_in[1].gl_Position.xy - gl_in[0].gl_Position.xy;
	vec2 normal = normalize(vec2(-tangent.y, tangent.x)) * halfWidth;
	
	vec4 original = gl_in[0].gl_Position;
		
	gs_out.texCoords = vec2(halfPixelU, 1.0f - halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.xy - normal.xy, original.z, original.w);
	EmitVertex();
	
	gs_out.texCoords = vec2(halfPixelU, halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.xy + normal.xy, original.z, original.w);
	EmitVertex();
	
	original = gl_in[1].gl_Position;
	
	gs_out.texCoords = vec2(1.0f - halfPixelU, 1.0f - halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.xy - normal.xy, original.z, original.w);
	EmitVertex();
	
	gs_out.texCoords = vec2(1.0f - halfPixelU, halfPixelV);
	gs_out.texID = texID;
	gl_Position = projMatrix * vec4(original.xy + normal.xy, original.z, original.w);
	EmitVertex();
}