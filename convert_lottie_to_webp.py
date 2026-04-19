#!/usr/bin/env python3
"""Convert image-sequence Lottie JSON files to animated WebP.

This pipeline is for Lottie files whose frames are embedded as
`data:image/webp;base64,...` assets. We extract the selected still frames and
assemble the animation with `webpmux` using NO_BLEND so transparent pixels do
not leave trails from previous frames.
"""

from __future__ import annotations

import argparse
import base64
import json
import subprocess
import tempfile
from pathlib import Path

DEFAULT_WEBPMUX = Path("/tmp/libwebp-tools/libwebp-1.2.1-linux-x86-64/bin/webpmux")
PANDA_DIR = Path("app/src/main/assets/animations/panda")
PANDA_MAPPINGS = [
    ("panda_eating_lottie.json", "panda_eating.webp"),
    ("panda_happy_lottie.json", "panda_happy.webp"),
    ("panda_hungry_lottie.json", "panda_hungry.webp"),
    ("panda_idle_lottie.json", "panda_idle.webp"),
    ("panda_interact_lottie.json", "panda_interact.webp"),
    ("panda_loop_sleeping.json", "panda_loop_sleeping.webp"),
    ("panda_sad_lottie.json", "panda_sad.webp"),
    ("panda_sleep_lottie.json", "panda_start_sleep.webp"),
    ("panda_thumbs_up_lottie.json", "panda_thumbs_up.webp"),
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", nargs="?")
    parser.add_argument("output", nargs="?")
    parser.add_argument(
        "--webpmux",
        default=str(DEFAULT_WEBPMUX),
        help="Path to the webpmux binary",
    )
    parser.add_argument(
        "--target-fps",
        type=int,
        default=15,
        help="Output frame rate. Defaults to 15 fps.",
    )
    parser.add_argument(
        "--all-panda",
        action="store_true",
        help="Convert all checked-in panda Lottie JSON files.",
    )
    return parser.parse_args()


def build_output_indices(frame_count: int, src_fps: int, target_fps: int) -> list[int]:
    if target_fps <= 0 or src_fps <= 0:
        raise ValueError("Frame rates must be positive")

    indices: list[int] = []
    step = src_fps / target_fps
    n = 0
    while True:
        index = int(n * step)
        if index >= frame_count:
            break
        indices.append(index)
        n += 1
    return indices


def build_durations(frame_count: int, target_fps: int) -> list[int]:
    durations: list[int] = []
    for i in range(frame_count):
        start_ms = round(i * 1000 / target_fps)
        end_ms = round((i + 1) * 1000 / target_fps)
        durations.append(max(1, end_ms - start_ms))
    return durations


def convert_image_sequence_lottie(
    json_path: Path,
    webp_path: Path,
    webpmux_path: Path,
    target_fps: int,
) -> None:
    data = json.loads(json_path.read_text())
    assets = data.get("assets", [])
    if not assets:
        raise ValueError(f"No assets found in {json_path}")
    if not str(assets[0].get("p", "")).startswith("data:image/"):
        raise ValueError(f"{json_path} is not an image-sequence Lottie with embedded frames")

    src_fps = int(data.get("fr", 30))
    output_indices = build_output_indices(len(assets), src_fps, target_fps)
    durations = build_durations(len(output_indices), target_fps)

    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        frame_paths: list[Path] = []
        for output_index, asset_index in enumerate(output_indices):
            payload = assets[asset_index].get("p", "")
            frame_data = base64.b64decode(payload.split(",", 1)[1])
            frame_path = temp_path / f"frame_{output_index:04d}.webp"
            frame_path.write_bytes(frame_data)
            frame_paths.append(frame_path)

        command = [str(webpmux_path)]
        for frame_path, duration in zip(frame_paths, durations):
            command.extend([
                "-frame",
                str(frame_path),
                f"+{duration}+0+0+0-b",
            ])
        command.extend([
            "-loop",
            "0",
            "-bgcolor",
            "0,0,0,0",
            "-o",
            str(webp_path),
        ])
        subprocess.run(command, check=True)

    size_kb = webp_path.stat().st_size // 1024
    print(
        f"Converted {json_path.name} -> {webp_path.name} "
        f"({size_kb} KB, src_fps={src_fps}, out_fps={target_fps}, frames={len(output_indices)})",
    )


def main() -> None:
    args = parse_args()
    webpmux_path = Path(args.webpmux)
    if not webpmux_path.is_file():
        raise FileNotFoundError(
            f"webpmux not found at {webpmux_path}. Download the libwebp tool bundle first.",
        )

    if args.all_panda:
        for src_name, dst_name in PANDA_MAPPINGS:
            convert_image_sequence_lottie(
                PANDA_DIR / src_name,
                PANDA_DIR / dst_name,
                webpmux_path,
                args.target_fps,
            )
        return

    if not args.input or not args.output:
        raise SystemExit("Provide INPUT and OUTPUT, or use --all-panda")

    convert_image_sequence_lottie(
        Path(args.input),
        Path(args.output),
        webpmux_path,
        args.target_fps,
    )


if __name__ == "__main__":
    main()
