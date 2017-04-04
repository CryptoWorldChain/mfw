package onight.tfw.ojpa.ordb.loader;

import mousio.etcd4j.requests.EtcdKeyDeleteRequest;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.requests.EtcdKeyPostRequest;
import mousio.etcd4j.requests.EtcdKeyPutRequest;
import mousio.etcd4j.responses.EtcdHealthResponse;

public interface OParams {

	EtcdHealthResponse getHealth();

	EtcdKeyPutRequest put(String key, String value);

	EtcdKeyPutRequest refresh(String key, Integer ttl);

	EtcdKeyPutRequest putDir(String dir);

	EtcdKeyPostRequest post(String key, String value);

	EtcdKeyDeleteRequest delete(String key);

	EtcdKeyDeleteRequest deleteDir(String dir);

	EtcdKeyGetRequest get(String key);

	EtcdKeyGetRequest getDir(String dir);

	EtcdKeyGetRequest getAll();

}