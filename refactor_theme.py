import os
import glob
import re

def refactor_colors():
    base_dir = r"c:\Users\murug\AndroidStudioProjects\BurnOutTracker\app\src"
    kt_files = glob.glob(os.path.join(base_dir, "**", "*.kt"), recursive=True)
    
    replacements = [
        # Precise hex replacements that are unambiguous
        (r'Color\(0xFFF9FAFB\)', 'ThemeColors.background'),
        (r'Color\(0xFFF3F4F6\)', 'ThemeColors.background'),
        (r'Color\(0xFF1F2937\)', 'ThemeColors.textPrimary'),
        (r'Color\(0xFF6B7280\)', 'ThemeColors.textSecondary'),
        (r'Color\(0xFF4B5563\)', 'ThemeColors.textSecondary'),
        (r'Color\(0xFF94A3B8\)', 'ThemeColors.textTertiary'),
        (r'Color\(0xFF9CA3AF\)', 'ThemeColors.textTertiary'),
        (r'Color\(0xFFE5E7EB\)', 'ThemeColors.border'),
        
        # Safe Color.White replacements
        (r'Modifier\.background\(Color\.White\)', 'Modifier.background(ThemeColors.card)'),
        (r'containerColor\s*=\s*Color\.White', 'containerColor = ThemeColors.card'),
        (r'cardColor\s*=\s*Color\.White', 'cardColor = ThemeColors.card'),
        (r'backgroundColor\s*=\s*Color\.White', 'backgroundColor = ThemeColors.card'),
        (r'Color\(0xFFFFFFFF\)', 'ThemeColors.card')
    ]
    
    import_stmt = 'import com.simats.burnouttracker.ui.theme.ThemeColors\n'
    
    files_modified = 0
    
    for file_path in kt_files:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        original_content = content
        
        # Do replacements
        for pattern, replacement in replacements:
            content = re.sub(pattern, replacement, content)
            
        if content != original_content:
            # Check if import is needed
            if 'ThemeColors' in content and 'import com.simats.burnouttracker.ui.theme.ThemeColors' not in content:
                # Insert import after package declaration
                content = re.sub(r'^(package .*?\n)', r'\1\n' + import_stmt, content, count=1, flags=re.MULTILINE)
                
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            files_modified += 1
            print(f"Modified: {os.path.basename(file_path)}")
            
    print(f"\nTotal files modified: {files_modified}")

if __name__ == '__main__':
    refactor_colors()
