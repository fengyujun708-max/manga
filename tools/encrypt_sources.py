#!/usr/bin/env python3
"""构建时加密源 JS 文件：XOR + Base64"""
import os, base64, sys

KEY = b'ManjieSourceKey2026'

def xor_encrypt(data: bytes) -> bytes:
    return bytes([b ^ KEY[i % len(KEY)] for i, b in enumerate(data)])

def encrypt_file(input_path: str, output_path: str):
    with open(input_path, 'rb') as f:
        raw = f.read()
    encrypted = xor_encrypt(raw)
    encoded = base64.b64encode(encrypted)
    with open(output_path, 'wb') as f:
        f.write(encoded)

if __name__ == '__main__':
    src_dir = sys.argv[1] if len(sys.argv) > 1 else 'assets/sources'
    count = 0
    for f in sorted(os.listdir(src_dir)):
        if not f.endswith('.js'):
            continue
        src = os.path.join(src_dir, f)
        dst = os.path.join(src_dir, f + '.enc')
        encrypt_file(src, dst)
        os.remove(src)  # 删除明文，只保留加密版
        count += 1
    print(f'Encrypted {count} files')
