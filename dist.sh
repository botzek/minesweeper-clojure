rm -rf dist
rm -rf target/release

npx shadow-cljs release app

rsync -av --exclude='js' public/* dist/
rsync -av --exclude '*.edn' target/release/* dist/

for file in $(find dist -type f)
do
	gzip $file
	touch $file
done

tar -czvf dist.tar.gz dist
rm -rf dist
