#!/bin/bash
# api_gen_py skill 安装脚本
# 用法: ./install.sh

set -e
SKILL_DIR="$(cd "$(dirname "$0")" && pwd)"
CLAUDE_SKILLS="$HOME/.claude/skills/api-gen-py"

echo "==> 安装 api_gen_py 到 Claude Code skills..."
mkdir -p "$CLAUDE_SKILLS"

# 复制 SKILL.md
cp "$SKILL_DIR/SKILL.md" "$CLAUDE_SKILLS/SKILL.md"
echo "  SKILL.md → $CLAUDE_SKILLS/SKILL.md"

# 安装 Python 包
echo "==> 安装 Python 包..."
cd "$SKILL_DIR"
pip install -e . 2>/dev/null || pip3 install -e . 2>/dev/null || python3 -m pip install -e .
echo "  done"

echo "==> api_gen_py 安装完成!"
echo "  Skill 目录: $CLAUDE_SKILLS"
echo "  CLI 命令: api-gen-py"
echo "  或直接: python3 -m api_gen_py"
