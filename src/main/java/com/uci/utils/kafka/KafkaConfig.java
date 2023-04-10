package com.uci.utils.kafka;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
	@Value("${spring.kafka.bootstrap-servers}")
	String kafkaServerUrl;

	@Autowired
	private AdminClient adminClient;
	
	/** 
	 * Set kafka admin client with kafka properties 
	 * Returns AdminClient
	 */
	@Bean
	public AdminClient kafkaAdminClient() {
		Properties properties = new Properties();
		properties.put("bootstrap.servers", kafkaServerUrl);
		properties.put("connections.max.idle.ms", 100000);
		properties.put("request.timeout.ms", 5000);
		return AdminClient.create(properties);
	}

	/** 
	 * Bean for Kafka health indicator, set health up/down of kafka server
	 * @return kafka health indicator
	 */
	@Bean
	public HealthIndicator kafkaHealthIndicator() {
		final DescribeClusterOptions describeClusterOptions = new DescribeClusterOptions().timeoutMs(1000);
		return () -> {
			try {
				final DescribeClusterResult describeCluster = adminClient.describeCluster(describeClusterOptions);
				final String clusterId = describeCluster.clusterId().get();
				final int nodeCount = describeCluster.nodes().get().size();
				ListTopicsOptions options = new ListTopicsOptions();
			    options.listInternal(true); // includes internal topics such as __consumer_offsets
			    ListTopicsResult topics = adminClient.listTopics(options);
			    Set<String> currentTopicList = topics.names().get();
			    final int currentTopicCount = currentTopicList.size();
			    
				return Health.up()
						.withDetail("clusterId", clusterId)
						.withDetail("nodeCount", nodeCount)
						.withDetail("topicsExists", currentTopicCount > 0)
						.build();
			} catch (InterruptedException | ExecutionException e) {
				return Health.down()
						.withException(e)
						.build();
			}
		};
	}
}
