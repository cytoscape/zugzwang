#version 450
//#extension GL_ARB_bindless_texture : require

in GS_OUT
{
	vec2 texCoords;
	flat int texID;
} fs_in;

out vec4 color;

void main(void)
{
	//sampler2D s = sampler2D(fs_in.texID);
	//float bla = float(handle[0]) / 255.0f;
	color = vec4(0.0f, 0.0f, 0.0f, 1.0f);
	//color = vec4(float(texID), 1.0f, 1.0f, 1.0f);
	
	//color = texture(handle[fs_in.texID], fs_in.texCoords);
}