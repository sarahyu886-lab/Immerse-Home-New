"""Reproducible meter-scale demo assets; never rewrites legacy furniture."""
import argparse
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import math
from pathlib import Path
import re

import numpy as np
from PIL import Image
import trimesh
from trimesh.visual.material import PBRMaterial
from trimesh.visual.texture import TextureVisuals

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
LEGACY = {
    "sofa-02": ("sofa.glb", [1.807, .709, .818]),
    "chair-01": ("ArmChair_01.glb", [.848, 1.065, .766]),
    "lamp-01": ("desk_lamp_arm_01.glb", [.202, .893, .614]),
}
GENERATED = {
    "rug-01": ("woven_rug.glb", [2, .015, 3]),
    "floor-lamp-01": ("floor_lamp.glb", [.45, 1.65, .45]),
    "tv-cabinet-01": ("tv_cabinet.glb", [1.8, .5, .4]),
}


def texture(kind):
    y, x = np.mgrid[:512, :512]
    rng = np.random.default_rng(27)
    noise = rng.normal(0, 1.5, (512, 512))
    if kind == "oak":
        grain = 5 * np.sin(x * .21 + 2 * np.sin(y * .024))
        grain += 2 * np.sin(x * .7 + np.sin(y * .08))
        boards = (x // (512 / 6)).astype(int)
        noise += grain + np.array([0, 7, -3, 4, -5, 2])[boards]
        noise[(x % (512 / 6)) < 1.8] -= 35
        noise[(y + (boards % 2) * 256) % 512 < 1.8] -= 30
        base = [173, 139, 99]
    elif kind == "weave":
        noise += 6 * np.sin(x * math.pi / 2) + 4 * np.sin(y * math.pi / 2)
        base = [194, 180, 151]
    else:
        base = [225, 221, 208]
    pixels = np.clip(np.array(base) + noise[..., None], 0, 255).astype("uint8")
    return Image.fromarray(pixels)


def material(name, color, roughness=.8, image=None, metallic=0):
    return PBRMaterial(name=name, baseColorFactor=color,
                       baseColorTexture=image, roughnessFactor=roughness,
                       metallicFactor=metallic, doubleSided=True)


def box(scene, name, size, center, mat, tile=(1, 1)):
    mesh = trimesh.creation.box(extents=size)
    # Split face vertices so each face has independent meter-space UVs.
    vertices = mesh.vertices[mesh.faces].reshape((-1, 3))
    faces = np.arange(len(vertices)).reshape((-1, 3))
    normals = np.repeat(mesh.face_normals, 3, axis=0)
    uv = []
    for p, n in zip(vertices, normals):
        axis = int(np.argmax(np.abs(n)))
        u, v = ((p[2], p[1]) if axis == 0 else
                (p[0], p[2]) if axis == 1 else (p[0], p[1]))
        uv.append([u / tile[0], v / tile[1]])
    mesh = trimesh.Trimesh(vertices=vertices, faces=faces, process=False)
    mesh.visual = TextureVisuals(uv=uv, material=mat)
    mesh.apply_translation(center)
    scene.add_geometry(mesh, node_name=name, geom_name=name)


def cylinder(scene, name, radius, height, center, mat):
    mesh = trimesh.creation.cylinder(radius=radius, height=height, sections=48)
    mesh.apply_transform(trimesh.transformations.rotation_matrix(math.pi / 2, [1, 0, 0]))
    mesh.apply_translation(center)
    mesh.visual = TextureVisuals(uv=np.zeros((len(mesh.vertices), 2)), material=mat)
    scene.add_geometry(mesh, node_name=name, geom_name=name)


def write_scene(scene, filename):
    data = scene.export(file_type="glb")
    (ASSETS / filename).write_bytes(data)


def create_models():
    oak = material("oak-demo-1.2m-tile", [255, 255, 255, 255], image=texture("oak"))
    weave = material("woven-fabric-demo", [255, 255, 255, 255], image=texture("weave"))
    border = material("warm-charcoal-binding", [81, 73, 60, 255])
    metal = material("brushed-brass", [156, 127, 77, 255], .36, metallic=.65)
    shade = material("warm-linen-shade", [238, 226, 194, 255], .85)
    rug = trimesh.Scene()
    box(rug, "binding", [2, .012, 3], [0, .006, 0], border)
    box(rug, "woven-center", [1.92, .003, 2.92], [0, .0135, 0], weave, (.3, .3))
    write_scene(rug, "woven_rug.glb")
    lamp = trimesh.Scene()
    cylinder(lamp, "weighted-base", .19, .04, [0, .02, 0], metal)
    cylinder(lamp, "stem", .014, 1.36, [0, .72, 0], metal)
    # An open frustum, not a solid cone, keeps the shade recognizable from below.
    n = 48
    verts = [[r * math.cos(i * 2 * math.pi / n), h, r * math.sin(i * 2 * math.pi / n)]
             for r, h in [(.225, 1.3), (.16, 1.65)] for i in range(n)]
    faces = []
    for i in range(n):
        j = (i + 1) % n
        faces.extend([[i, j, i + n], [j, j + n, i + n]])
    mesh = trimesh.Trimesh(vertices=verts, faces=faces, process=False)
    mesh.visual = TextureVisuals(uv=np.zeros((len(verts), 2)), material=shade)
    lamp.add_geometry(mesh, node_name="linen-shade")
    write_scene(lamp, "floor_lamp.glb")
    cabinet = trimesh.Scene()
    for x in [-.78, .78]:
        for z in [-.13, .13]:
            box(cabinet, f"leg-{x}-{z}", [.045, .12, .045], [x, .06, z], metal)
    box(cabinet, "bottom", [1.8, .025, .4], [0, .1325, 0], oak, (1.2, 1.2))
    box(cabinet, "top", [1.8, .025, .4], [0, .4875, 0], oak, (1.2, 1.2))
    box(cabinet, "back", [1.8, .33, .018], [0, .31, -.191], oak)
    for x in [-.888, -.3, .3, .888]:
        box(cabinet, f"divider-{x}", [.024, .33, .4], [x, .31, 0], oak)
    for x in [-.59, .59]:
        box(cabinet, f"door-{x}", [.56, .315, .024], [x, .31, .188], oak)
    box(cabinet, "open-shelf", [.576, .018, .37], [0, .3, 0], oak)
    write_scene(cabinet, "tv_cabinet.glb")


def create_room(args):
    source = ROOT / "app/src/main/java/com/pico/welcomespace/domain/model/FloorPlanRoom.kt"
    text = source.read_text()
    def constant(name):
        return float(re.search(rf"const val {name} = ([\d.]+)f", text)[1])
    w, d, h = [constant(k) for k in ["PlanWidthMeters", "PlanDepthMeters", "RoomHeightMeters"]]
    t = constant("WallThickness")
    room = trimesh.Scene()
    floor_image = Image.open(args.floor_texture).convert("RGB") if args.floor_texture else texture("oak")
    wall_image = Image.open(args.wall_texture).convert("RGB") if args.wall_texture else texture("plaster")
    floor = material("floor-meter-tiled", [255] * 4, image=floor_image)
    wall = material("wall-meter-tiled", [255] * 4, image=wall_image)
    floor_tile = args.floor_tile or (1.2, 1.2)
    wall_tile = args.wall_tile or (1, 1)
    box(room, "floor", [w + 2*t, .04, d + 2*t], [0, -.02, 0], floor, floor_tile)
    box(room, "ceiling", [w + 2*t, .04, d + 2*t], [0, h + .02, 0], wall, wall_tile)
    for z in [-1, 1]:
        box(room, f"wall-z-{z}", [w + 2*t, h, t], [0, h/2, z*(d+t)/2], wall, wall_tile)
    for x in [-1, 1]:
        box(room, f"wall-x-{x}", [t, h, d], [x*(w+t)/2, h/2, 0], wall, wall_tile)
    write_scene(room, "standard_room.glb")
    (ASSETS / "profiles/room-surfaces.json").write_text(json.dumps({
        "interiorMeters": [w, h, d],
        "floor": {"tileMeters": floor_tile, "source": "user_supplied" if args.floor_texture else "procedural_demo"},
        "walls": {"tileMeters": wall_tile, "source": "user_supplied" if args.wall_texture else "procedural_demo"},
        "uvPolicy": "surface position in meters divided by texture coverage meters",
        "normalMap": None, "roughness": .8
    }, indent=2) + "\n")


def profile(asset_id, filename, target, bbox, legacy):
    raw = [bbox["dimensions"][k] for k in ["width", "height", "depth"]]
    axis = int(np.argmax(target))
    scale = target[axis] / raw[axis]
    actual = [v * scale for v in raw]
    deviation = max(abs(a / b - 1) for a, b in zip(actual, target))
    if deviation > .25:
        raise ValueError(f"{asset_id}: aspect-ratio deviation exceeds 25%")
    catalog = (ROOT / "app/src/main/java/com/pico/welcomespace/data/repository/FakeShopRepository.kt").read_text()
    record = re.search(rf'id = "{re.escape(asset_id)}",(.*?)(?=\n\s*\),?\n)', catalog, re.S)[1]
    name = re.search(r'name = "([^"]+)"', record)[1]
    category = re.search(r'category = ShopCategory.(\w+)', record)[1]
    return {
        "schemaVersion": 1, "assetId": asset_id, "name": name, "category": category, "file": filename,
        "sha256": hashlib.sha256((ASSETS / filename).read_bytes()).hexdigest(),
        "source": "legacy_bundled" if legacy else "procedural_demo",
        "format": "GLB", "originalUnit": "meter", "unitEvidence": "glTF 2.0 specification",
        "originalSizeMeters": raw, "boundingBox": {**bbox, "space": "asset scene, node transforms applied"},
        "declaredSizeMeters": target, "realFurnitureSizeMeters": None,
        "standardSizeMeters": actual, "coordinateSystem": "right-handed",
        "upAxis": "+Y", "pivot": "runtime wrapper: bottom center; original file unchanged",
        "scaleRecord": {"unitConversion": 1, "uniformCalibration": scale,
                        "baked": False, "appliedOnceToFreshAssetChild": True},
        "dimensionConfidence": "unverified_nominal" if legacy else "design_dimensions",
        "dimensionSource": "existing catalog, no physical specification" if legacy else "procedural geometry",
        "preview": {"type": "runtime_SpatialModelView", "imageFile": None},
        "materials": "embedded glTF materials and images, original preserved",
        "validationStatus": "geometry_checked_real_size_unverified" if legacy else "design_geometry_checked",
        "ratioDeviation": deviation,
        "originalImportTime": None,
        "profileCreatedAt": datetime.now(timezone.utc).isoformat()
    }


def write_transforms():
    profiles = [json.loads((ASSETS / "profiles" / f"{key}.json").read_text())
                for key in {**LEGACY, **GENERATED}]
    output = ROOT / ".spatialsdk"
    output.mkdir(exist_ok=True)
    scene = {
        "scene_context": "Interior 4 x 5 x 2.5 m. Asset-local normalization only; furniture is placed interactively, not spawned at these coordinates.",
        "scene_dimensions_meters": {"width": 4, "depth": 5, "height": 2.5},
        "elements": [{
            "asset_id": p["assetId"],
            "original_bounding_box_meters": p["boundingBox"],
            "transformation": {
                "translation": [
                    -(p["boundingBox"]["min"][0] + p["boundingBox"]["max"][0]) / 2 * p["scaleRecord"]["uniformCalibration"],
                    -p["boundingBox"]["min"][1] * p["scaleRecord"]["uniformCalibration"],
                    -(p["boundingBox"]["min"][2] + p["boundingBox"]["max"][2]) / 2 * p["scaleRecord"]["uniformCalibration"]
                ],
                "rotation": [0, 0, 0],
                "scale": [p["scaleRecord"]["uniformCalibration"]] * 3
            },
            "dimension_confidence": p["dimensionConfidence"]
        } for p in profiles]
    }
    (output / "scene_transforms.json").write_text(json.dumps(scene, indent=2) + "\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bbox-tool", required=True, type=Path)
    parser.add_argument("--only-room", action="store_true")
    parser.add_argument("--floor-texture", type=Path)
    parser.add_argument("--wall-texture", type=Path)
    parser.add_argument("--floor-tile", type=float, nargs=2, metavar=("WIDTH_M", "LENGTH_M"))
    parser.add_argument("--wall-tile", type=float, nargs=2, metavar=("WIDTH_M", "HEIGHT_M"))
    args = parser.parse_args()
    for image, tile in [(args.floor_texture, args.floor_tile), (args.wall_texture, args.wall_tile)]:
        if image and not tile:
            parser.error("User textures require explicit coverage dimensions in meters.")
        if tile and not all(math.isfinite(v) and v > 0 for v in tile):
            parser.error("Texture coverage must be positive finite meters.")
    spec = importlib.util.spec_from_file_location("calculate_bbox", args.bbox_tool)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    before = {file: hashlib.sha256((ASSETS / file).read_bytes()).hexdigest()
              for file, _ in LEGACY.values()}
    if not args.only_room:
        create_models()
    create_room(args)
    for asset_id, (filename, size) in {**LEGACY, **GENERATED}.items():
        bbox = module.calculate_bbox_trimesh(str(ASSETS / filename))
        p = profile(asset_id, filename, size, bbox, asset_id in LEGACY)
        (ASSETS / "profiles" / f"{asset_id}.json").write_text(json.dumps(p, indent=2) + "\n")
        print(asset_id, bbox["dimensions"], p["validationStatus"])
    assert before == {file: hashlib.sha256((ASSETS / file).read_bytes()).hexdigest()
                      for file, _ in LEGACY.values()}
    write_transforms()
    print("Legacy asset hashes unchanged.")


if __name__ == "__main__":
    main()
