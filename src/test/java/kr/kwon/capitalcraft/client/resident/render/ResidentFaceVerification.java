package kr.kwon.capitalcraft.client.resident.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;

/** Objective checks: one undecorated cuboid shell with painted, correctly mapped features. */
final class ResidentFaceVerification {
    static void check(JsonObject data, BufferedImage texture, ModelPart root) {
        boolean seia=data.get("texture").getAsString().contains("seia");
        JsonObject head = null;
        int heads = 0;
        for (var value : data.getAsJsonArray("objects")) {
            var object = value.getAsJsonObject();
            String name = object.get("name").getAsString();
            require(!name.matches("(soft_face|eye_white.*|iris.*|pupil.*|catchlight.*|eyelash.*|sleepy_lash.*|eyebrow.*|blush.*|small_mouth|nose)"),
                "Facial features must be painted, not separate geometry: " + name);
            if (name.equals("flat_face")) { head=object; heads++; }
        }
        require(heads==1 && head!=null, "Exactly one cuboid face shell");
        require(head.get("bone").getAsString().equals("head"), "Face must follow head rotation");
        var vertices=head.getAsJsonArray("vertices");
        if(seia) require(vertices.size()==8 && head.getAsJsonArray("faces").size()==6,
            "Seia cuboid: eight vertices, six faces");
        else require(vertices.size()==12 && head.getAsJsonArray("faces").size()==10,
            "Mari mirrored Blender cuboid: twelve evaluated vertices, ten faces");
        for(int axis=0;axis<3;axis++) {
            Set<Float> positions=new HashSet<>();
            for(var vertex:vertices) positions.add(vertex.getAsJsonArray().get(axis).getAsFloat());
            int expectedPositions=!seia && axis==0 ? 3 : 2;
            require(positions.size()==expectedPositions, "No cheeks, bevels, curves or nose protrusion");
        }
        float[] low={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE};
        float[] high={-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        for(var vertex:vertices) for(int axis=0;axis<3;axis++) {
            float value=vertex.getAsJsonArray().get(axis).getAsFloat();
            low[axis]=Math.min(low[axis],value); high[axis]=Math.max(high[axis],value);
        }
        float width=high[0]-low[0],height=high[1]-low[1],depth=high[2]-low[2];
        float expectedWidth=seia?.7198F:.54972F;
        float expectedHeight=seia?.6572F:.43554F;
        float expectedDepth=seia?.638F:.5357F;
        require(Math.abs(width-expectedWidth)<1e-4F, "Imported head width");
        require(Math.abs(height-expectedHeight)<1e-4F, "Imported head height");
        require(Math.abs(depth-expectedDepth)<1e-4F, "Imported head depth");
        if(seia) require(Math.max(width,Math.max(height,depth))/Math.min(width,Math.min(height,depth))<1.14F,
            "Seia head dimensions must remain cube-like");
        int textured=0;
        JsonObject painted=null;
        Set<String> expected=new HashSet<>();
        for(var value:head.getAsJsonArray("faces")) {
            var face=value.getAsJsonObject();
            require(face.getAsJsonArray("indices").size()==4, "Cuboid face must be a quad");
            if(face.has("uv")) {
                painted=face;textured++;
                Set<String> mapped=new HashSet<>();
                for(var uv:face.getAsJsonArray("uv")) {
                    var point=uv.getAsJsonArray();
                    mapped.add(key(point.get(0).getAsFloat(),point.get(1).getAsFloat()));
                }
                if(expected.isEmpty())expected.addAll(mapped);
                else require(mapped.equals(expected), "Both Mari mirror halves must sample the same eye tile");
            }
        }
        require(textured==(seia?1:2) && painted!=null, "Only the front carries painted facial UVs");
        float front=-Float.MAX_VALUE;
        for(var v:vertices) front=Math.max(front,v.getAsJsonArray().get(2).getAsFloat());
        float minU=1,maxU=0,minV=1,maxV=0;
        for(var value:head.getAsJsonArray("faces")) {
            var face=value.getAsJsonObject();
            if(!face.has("uv"))continue;
            for(int i=0;i<4;i++) {
                int index=face.getAsJsonArray("indices").get(i).getAsInt();
                require(vertices.get(index).getAsJsonArray().get(2).getAsFloat()==front, "Paint on the front, not the back");
                var uv=face.getAsJsonArray("uv").get(i).getAsJsonArray();
                float u=uv.get(0).getAsFloat(),v=uv.get(1).getAsFloat();
                minU=Math.min(minU,u);maxU=Math.max(maxU,u);minV=Math.min(minV,v);maxV=Math.max(maxV,v);
                float x=vertices.get(index).getAsJsonArray().get(0).getAsFloat();
                float y=vertices.get(index).getAsJsonArray().get(1).getAsFloat();
                if(seia) require((x<0)==(u<.625F), "Seia face texture must not be mirrored");
                else require((Math.abs(x)>.1F)==(u>.75F), "Mari mirror halves must map centre-to-edge");
                require((y>1.88F)==(v<.3125F), "Face texture must not be upside down");
            }
        }
        require(maxU-minU>(seia?.4F:.24F) && maxV-minV>.4F, "Face UVs cover artwork, not a palette cell");
        int[] matching={0};
        root.visit(new PoseStack(),(pose,path,index,cube)-> {
            Set<String> actual=new HashSet<>();
            for(var vertex:cube.polygons[0].vertices()) actual.add(key(vertex.u(),vertex.v()));
            if(actual.equals(expected)) matching[0]++;
        });
        require(matching[0]==(seia?1:2), "Native ModelPart must preserve every painted face UV corner");
        int x=96,y=16,w=128,h=128;
        int skin=Integer.parseInt(data.getAsJsonObject("palette").get("skin").getAsString().substring(1),16);
        for(double[] region:seia?new double[][]{{.1,.48,.43,.74},{.57,.48,.9,.74}}
            :new double[][]{{.625,.55,.96,.86}}) {
            int changed=0;
            for(int py=y+(int)(region[1]*h);py<y+(int)(region[3]*h);py++)
                for(int px=x+(int)(region[0]*w);px<x+(int)(region[2]*w);px++)
                    if((texture.getRGB(px,py)&0xffffff)!=skin)changed++;
            require(changed>0, "Painted eye artwork must be present in every mapped tile");
        }
        for(int py=seia?91:110;py<128;py++) for(int px=0;px<128;px++) {
            require((texture.getRGB(x+px,y+py)&0xffffff)==skin,
                "No nose or mouth pixels: the entire lower face must be plain skin");
        }
        if(!seia) {
            int irisPixels=0,whitePixels=0;
            for(int py=16;py<144;py++) for(int px=160;px<224;px++) {
                int colour=texture.getRGB(px,py)&0xffffff;
                if(colour!=skin) require(px>=174 && px<=219 && py>=84 && py<=125,
                    "Imported eye and eyelash artwork must have skin margins on all four sides");
                if(colour==0x468c9b)irisPixels++;
                if(colour==0xffffff)whitePixels++;
            }
            require(irisPixels>500 && whitePixels>150,
                "Imported Mari eye must retain its large teal iris and white sclera/highlight");
            require((texture.getRGB(193,91)&0xffffff)==0x35343e,
                "Imported Mari upper eyelid must remain solid and thick");
            require((texture.getRGB(215,97)&0xffffff)==0x35343e,
                "Imported Mari outer eyelash wing must remain complete");
        }
        // Seia keeps the existing rectangular-pupil reference artwork.
        for(int cx:seia?new int[]{36,92}:new int[0]) {
            int minX=128,maxX=-1,coloured=0,eyeMin=128,eyeMax=-1;
            int upper=seia?72:70, lower=seia?88:89;
            for(int py=upper+3;py<=lower;py++) for(int px=cx-24;px<=cx+24;px++) {
                if((texture.getRGB(x+px,y+py)&0xffffff)!=skin) {
                    eyeMin=Math.min(eyeMin,px);eyeMax=Math.max(eyeMax,px);
                }
            }
            require(eyeMax-eyeMin+1>=39 && eyeMax-eyeMin+1<=41,
                "Each eye opening must occupy approximately one third of the face width");
            for(int py=upper+7;py<lower-1;py++) for(int px=cx-12;px<=cx+12;px++) {
                int colour=texture.getRGB(x+px,y+py),r=(colour>>16)&255,g=(colour>>8)&255,b=colour&255;
                boolean iris=seia ? r>=145 && b>g+12 && r>g+8 : g>r+25 && b>r+25;
                if(iris) { minX=Math.min(minX,px);maxX=Math.max(maxX,px);coloured++; }
            }
            require(coloured>=20 && maxX-minX+1<=12, "Both painted irises must be narrow and visible");
            int pupil=seia?0x534051:0x29404d;
            for(int py=upper+7;py<lower-1;py++) {
                require((texture.getRGB(x+cx,y+py)&0xffffff)==pupil,
                    "Pupil must be a solid vertical rectangle, without white sparkle dots");
            }
            int ink=seia?0x514049:0x39333e;
            for(int py=upper;py<upper+3;py++) {
                require((texture.getRGB(x+cx,y+py)&0xffffff)==ink,
                    "Upper eyelid must have a thick solid angular band");
            }
            int side=cx<64?-1:1;
            require((texture.getRGB(x+cx+side*18,y+upper-3)&0xffffff)==ink,
                "Upper eyelid must have a pointed outer wing");
            int outer=cx+side*22;
            require((texture.getRGB(x+outer,y+upper-4)&0xffffff)==ink,
                "The complete outer eyelash wing must remain visible");
            int tip=cx+side*24;
            require(tip>=12 && tip<=116, "Eyelash antialiasing safety margin");
        }
        ResidentMeshUVs legacy=new ResidentMeshUVs(new JsonObject());
        JsonObject plain=JsonParser.parseString("{\"indices\":[0,1,2,3]}").getAsJsonObject();
        require(legacy.at(plain,0,0)[0]==3/64F, "Legacy 64px palette meshes stay compatible");
        JsonObject invalid=painted.deepCopy();
        invalid.getAsJsonArray("uv").get(0).getAsJsonArray().set(0,new com.google.gson.JsonPrimitive(2));
        try { new ResidentMeshUVs(data).at(invalid,0,0); throw new AssertionError("Out-of-range UV accepted"); }
        catch(IllegalArgumentException intended) { /* reject broken resource-pack UVs */ }
        System.out.println(seia
            ? "PASS: Seia reference block eyes; rectangular pupils; thick lids; no nose/mouth; native UVs"
            : "PASS: imported Mari mirrored face, teal eye and complete eyelash artwork; native UVs");
    }

    private static String key(float u,float v) { return Math.round(u*65536)+","+Math.round(v*65536); }
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
