target_files=$(grep -r 'new CancellableTask(' . --include=*.java | awk -F: '{print $1}' | sort | uniq)
sed -i '/import org.elasticsearch.tasks.CancellableTask;/a\import org.elasticsearch.tasks.BaseCancellableTask;' $target_files
sed -i "s/new CancellableTask(/new BaseCancellableTask(/g" $target_files
