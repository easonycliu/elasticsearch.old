root_dir=/home/eason/AutoCancelProject/

USER_ID=$(id -u) GROUP_ID=$(id -g) AUTOCANCEL_HOME=$root_dir DEFAULT_POLICY=multi_objective_policy PREDICT_PROGRESS=false docker compose -f $root_dir/autocancel_exp/elasticsearch_exp/cluster/single_node.yml down && \
sudo chown -R $(id -u):$(id -g) $root_dir/elasticsearch/build/distribution/local/elasticsearch-8.9.0-SNAPSHOT && \
curr_wd=$(pwd) && \
cd $root_dir/elasticsearch && \
./gradlew localDistro && \
cd $curr_wd && \
USER_ID=$(id -u) GROUP_ID=$(id -g) AUTOCANCEL_HOME=$root_dir DEFAULT_POLICY=multi_objective_policy PREDICT_PROGRESS=false docker compose -f $root_dir/autocancel_exp/elasticsearch_exp/cluster/single_node.yml up
