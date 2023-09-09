root_dir=/home/ubuntu/AutocancelProject
user_name=ubuntu

docker-compose --compatibility -f $root_dir/autocancel_exp/cluster/single_node.yml down && \
sudo chown -R $user_name:$user_name $root_dir/elasticsearch/build/distribution/local/elasticsearch-8.9.0-SNAPSHOT && \
curr_wd=$(pwd) && \
cd $root_dir/elasticsearch && \
./gradlew localDistro && \
cd $curr_wd && \
sudo chown -R 1000:1000 $root_dir/elasticsearch/build/distribution/local/elasticsearch-8.9.0-SNAPSHOT && \
docker-compose --compatibility -f $root_dir/autocancel_exp/cluster/single_node.yml up