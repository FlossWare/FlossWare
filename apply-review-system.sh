#!/bin/bash
# Apply automated code review system to all FlossWare projects

set -euo pipefail

SOURCE_PROJECT="/home/sfloess/Development/github/FlossWare/platform-java"
PROJECTS_DIR="/home/sfloess/Development/github/FlossWare"

echo "╔════════════════════════════════════════╗"
echo "║  Apply Review System to All Projects   ║"
echo "╚════════════════════════════════════════╝"
echo ""

# Find all git repositories
PROJECTS=$(find "$PROJECTS_DIR" -maxdepth 1 -type d -name "*" ! -name "." ! -name ".." -exec test -d "{}/.git" \; -print | sort)

COUNT=0
TOTAL=$(echo "$PROJECTS" | wc -l)

for PROJECT in $PROJECTS; do
    COUNT=$((COUNT + 1))
    PROJECT_NAME=$(basename "$PROJECT")

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "[$COUNT/$TOTAL] Processing: $PROJECT_NAME"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    cd "$PROJECT"

    # Skip if already has .claude/START_HERE.md
    if [ -f ".claude/START_HERE.md" ]; then
        echo "✓ Already configured, skipping"
        echo ""
        continue
    fi

    # Create directories
    mkdir -p .claude/scripts .claude/review-output

    # Detect project language
    PYTHON_COUNT=$(find . -name "*.py" -type f ! -path "./.git/*" 2>/dev/null | wc -l)
    JAVA_COUNT=$(find . -name "*.java" -type f ! -path "./.git/*" 2>/dev/null | wc -l)
    SHELL_COUNT=$(find . -type f \( -name "*.sh" -o -name "*.bash" \) ! -path "./.git/*" 2>/dev/null | wc -l)

    echo "Language detection:"
    echo "  Python: $PYTHON_COUNT files"
    echo "  Java:   $JAVA_COUNT files"
    echo "  Shell:  $SHELL_COUNT files"

    # Copy appropriate code_review.sh based on language
    if [ $JAVA_COUNT -gt 0 ] && [ $PYTHON_COUNT -gt 0 ]; then
        echo "  Type: Java + Python (hybrid)"
        cp "$SOURCE_PROJECT/.claude/scripts/code_review.sh" .claude/scripts/
    elif [ $JAVA_COUNT -gt 0 ]; then
        echo "  Type: Java"
        cp "$SOURCE_PROJECT/.claude/scripts/code_review.sh" .claude/scripts/
    elif [ $PYTHON_COUNT -gt 0 ] || [ $SHELL_COUNT -gt 0 ]; then
        echo "  Type: Python/Shell"
        cp "/home/sfloess/Development/github/FlossWare/VirtOS/.claude/scripts/code_review.sh" .claude/scripts/
    else
        echo "  Type: Unknown/Other - using Python/Shell template"
        cp "/home/sfloess/Development/github/FlossWare/VirtOS/.claude/scripts/code_review.sh" .claude/scripts/
    fi

    # Copy common scripts
    cp "$SOURCE_PROJECT/.claude/scripts/create_review_issues.py" .claude/scripts/
    cp "$SOURCE_PROJECT/.claude/scripts/review_loop.sh" .claude/scripts/

    # Copy auto_fix_and_push.sh and update for SSH vs HTTPS remote
    GIT_REMOTE=$(git remote get-url origin 2>/dev/null || echo "")
    if [[ "$GIT_REMOTE" == git@* ]]; then
        # SSH remote - use origin
        cp "/home/sfloess/Development/github/FlossWare/VirtOS/.claude/scripts/auto_fix_and_push.sh" .claude/scripts/
    else
        # HTTPS remote - use github
        cp "$SOURCE_PROJECT/.claude/scripts/auto_fix_and_push.sh" .claude/scripts/
    fi

    # Copy settings.json
    cp "$SOURCE_PROJECT/.claude/settings.json" .claude/settings.json

    # Copy START_HERE.md and update project name
    cp "$SOURCE_PROJECT/.claude/START_HERE.md" .claude/START_HERE.md
    sed -i "s/platform-java/$PROJECT_NAME/g" .claude/START_HERE.md
    sed -i "s/FlossWare platform-java/FlossWare $PROJECT_NAME/g" .claude/START_HERE.md

    # Make scripts executable
    chmod +x .claude/scripts/*.sh .claude/scripts/*.py

    # Add .gitignore entries if not present
    if ! grep -q "\.log" .gitignore 2>/dev/null; then
        echo "*.log" >> .gitignore
        echo "logs/" >> .gitignore
    fi

    echo "✓ Review system installed"
    echo ""
done

echo "╔════════════════════════════════════════╗"
echo "║        Installation Complete!          ║"
echo "╚════════════════════════════════════════╝"
echo ""
echo "Processed $TOTAL projects"
echo ""
echo "To start review on any project:"
echo "  cd <project>"
echo "  ./.claude/scripts/review_loop.sh"
echo ""
