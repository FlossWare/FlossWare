#!/bin/bash
#
# Rename J-prefixed classes and update all imports
#

set -e

FLOSSWARE_ROOT="/home/sfloess/Development/github/FlossWare"

echo "========================================"
echo "Renaming J-prefixed Classes"
echo "========================================"

# Function to rename a class and update all references
rename_class() {
    local old_name="$1"
    local new_name="$2"
    local project_dir="$3"
    local package_path="$4"  # e.g., "org.flossware.classloader"

    echo ""
    echo "Processing: $old_name -> $new_name in $(basename $project_dir)"

    # Find the file
    local old_file=$(find "$project_dir" -name "${old_name}.java" -type f | head -1)

    if [[ -z "$old_file" ]]; then
        echo "  ⚠️  File not found: ${old_name}.java"
        return 1
    fi

    echo "  Found: $old_file"

    # Calculate new file path
    local dir=$(dirname "$old_file")
    local new_file="${dir}/${new_name}.java"

    # Rename the file using git if in a git repo, otherwise regular mv
    cd "$project_dir"
    if [[ -d .git ]]; then
        git mv "$old_file" "$new_file" 2>/dev/null || mv "$old_file" "$new_file"
    else
        mv "$old_file" "$new_file"
    fi

    echo "  ✓ Renamed file"

    # Update class declaration and references in the renamed file
    sed -i "s/\\bpublic class ${old_name}\\b/public class ${new_name}/g" "$new_file"
    sed -i "s/\\bpublic final class ${old_name}\\b/public final class ${new_name}/g" "$new_file"
    sed -i "s/\\bpublic abstract class ${old_name}\\b/public abstract class ${new_name}/g" "$new_file"
    sed -i "s/\\bprivate ${old_name}(/private ${new_name}(/g" "$new_file"
    sed -i "s/\\bpublic ${old_name}(/public ${new_name}(/g" "$new_file"
    sed -i "s/\\bprotected ${old_name}(/protected ${new_name}(/g" "$new_file"
    sed -i "s/\\bnew ${old_name}(/new ${new_name}(/g" "$new_file"
    sed -i "s/\\b${old_name}\\.builder()/${new_name}.builder()/g" "$new_file"
    sed -i "s/\\b${old_name}\\.class/${new_name}.class/g" "$new_file"
    sed -i "s/\"${old_name}\"/\"${new_name}\"/g" "$new_file"
    sed -i "s/\\[${old_name} /[${new_name} /g" "$new_file"

    echo "  ✓ Updated class declaration"

    # Update all Java files in ALL FlossWare projects
    echo "  Updating imports and references across all projects..."
    find "$FLOSSWARE_ROOT" -name "*.java" -type f -exec sed -i \
        -e "s/\\bimport ${package_path}\\.${old_name};/import ${package_path}.${new_name};/g" \
        -e "s/\\b${old_name}\\.builder()/${new_name}.builder()/g" \
        -e "s/\\b${old_name} /${new_name} /g" \
        -e "s/<${old_name}>/<${new_name}>/g" \
        -e "s/(${old_name} /(${new_name} /g" \
        -e "s/, ${old_name}\\b/, ${new_name}/g" \
        -e "s/\\bnew ${old_name}(/new ${new_name}(/g" \
        -e "s/\\b${old_name}\\.class/${new_name}.class/g" \
        -e "s/\"${old_name}\"/\"${new_name}\"/g" \
        -e "s/\\[${old_name} /[${new_name} /g" \
        -e "s/\\[${old_name}\\]\\[/${new_name}][/g" \
        -e "s/\\b${old_name}\\[\\]/${new_name}[]/g" \
        {} +

    echo "  ✓ Updated all references"
}

# classloader-java
echo ""
echo "=== classloader-java ==="
rename_class "JClassLoader" "ApplicationClassLoader" "$FLOSSWARE_ROOT/classloader-java" "org.flossware.classloader"

# commons-java
echo ""
echo "=== commons-java ==="
rename_class "JCommonsIOException" "CommonsIOException" "$FLOSSWARE_ROOT/commons-java" "org.flossware.commons.io"

# remote-java
echo ""
echo "=== remote-java ==="
rename_class "JRemoteClient" "RemoteClient" "$FLOSSWARE_ROOT/remote-java" "org.flossware.remote"
rename_class "JRemoteServer" "RemoteServer" "$FLOSSWARE_ROOT/remote-java" "org.flossware.remote"

# nexus-java
echo ""
echo "=== nexus-java ==="
rename_class "JNexus" "Nexus" "$FLOSSWARE_ROOT/nexus-java" "org.flossware.nexus"
rename_class "JNexusAWT" "NexusAWT" "$FLOSSWARE_ROOT/nexus-java" "org.flossware.nexus"
rename_class "JNexusSwing" "NexusSwing" "$FLOSSWARE_ROOT/nexus-java" "org.flossware.nexus"
rename_class "JNexusUI" "NexusUI" "$FLOSSWARE_ROOT/nexus-java" "org.flossware.nexus"

# curses-java
echo ""
echo "=== curses-java ==="
rename_class "JButton" "Button" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JCheckbox" "Checkbox" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JCheckboxGroup" "CheckboxGroup" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JChoice" "Choice" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JComboBox" "ComboBox" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JDialog" "Dialog" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JFileDialog" "FileDialog" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JFrame" "Frame" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JGridLayout" "GridLayout" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JIndeterminateProgress" "IndeterminateProgress" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JLabel" "Label" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JList" "ListComponent" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JMenu" "Menu" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JMenuBar" "MenuBar" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JMenuItem" "MenuItem" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JPanel" "Panel" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JProgressBar" "ProgressBar" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JScrollBar" "ScrollBar" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JScrollPane" "ScrollPane" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JSeparator" "Separator" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JSlider" "Slider" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JSplitPane" "SplitPane" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JStatusBar" "StatusBar" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JTabbedPane" "TabbedPane" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JTable" "Table" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JTextArea" "TextArea" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JTextField" "TextField" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"
rename_class "JToolBar" "ToolBar" "$FLOSSWARE_ROOT/curses-java" "org.flossware.curses.api"

echo ""
echo "========================================"
echo "✓ All classes renamed successfully"
echo "========================================"
echo ""
echo "Next steps:"
echo "1. Review changes in each project"
echo "2. Run mvn clean test in each project"
echo "3. Commit changes to each project"
