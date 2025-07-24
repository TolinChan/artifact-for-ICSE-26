import os

def count_lines_of_code(directory):
    file_lines = {}

    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".sol"):  # Only consider Python files
                file_path = os.path.join(root, file)
                with open(file_path, "r") as f:
                    lines = f.readlines()
                    file_lines[file_path] = len(lines)

    return file_lines

directory = "./"
output_file = "./loc_count.txt"

file_lines = count_lines_of_code(directory)

with open(output_file, "w") as f:
    for file_path, lines in file_lines.items():
        f.write(f"{file_path[2:-4]}: {lines}\n")
