echo "experiment results" > results.out
for file in ./input/*.py
do
  echo "sythesizing $file"
  python3 "$file" >> results.out
done