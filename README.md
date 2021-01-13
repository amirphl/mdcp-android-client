## mdcp-android-client

- open `src/main/res/xml/network_security_config.xml` and replace `192.168.1.3` with mdcp server ip
- open `src/main/res/values/strings.xml` and replace `192.168.1.3` with mdcp server ip
- the app will try to connect to http://mdcp_server_ip:7979 to upload a task result and tcp://mdcp_server_ip:1883 to connect to mqtt broker to register itself,
so make sure that ports 7979 and 1883 are not blocked by server firewall and are accessible
