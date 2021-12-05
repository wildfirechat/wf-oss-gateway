## 野火对象存储网关
野火专业版IM服务只能支持少数几种对象存储服务，为了满足客户接入自有对象存储服务的需要，可以启用野火对象存储网关。客户端上传文件时上传到网关，网关把文件上传到客户自由对象存储。下载文件时直接从客户对象存储服务下载。

## 使用网关的好处
1. 可以对接任意对象存储服务，既可以对接私有对象存储服务，也可以对接云存储服务。
2. 上传过程经过加密，而且是使用用户的网络密钥进行加密，更加安全。

## 工作流程
### 上传流程
![oss-gateway_upload](http://media.wfcoss.cn/firechat/oss_gateway_upload.png)

如图，客户有自己的文件存储服务，可以是常见的FastDFS或HDFS或其他任意私有对象存储或者云存储，后面简称第三方存储服务。部署野火对象存储网关（OSS Gateway）和野火IM服务（IM Server），客户端（Client）来上传文件。上传经过如下步骤：
1. 客户端先请求IM服务分配上传token。
2. 服务器签名生成上传token返回给客户端
3. 客户端使用自己的密钥加密数据并和token一起上传到对象存储网关服务。
4. 对象存储网关服务校验上传token成功后，请求数据库该用户的密钥。
5. 数据库返回该用户的密钥，对象存储网关服务使用此密钥解密数据。
6. 对象存储网关服务把解密的数据发送到第三方存储服务。
7. 第三方对象存储服务返回上传成功的响应。
8. 对象存储服务返回客户端上传成功。

这里面有几个技术要点：
1. IM服务生成token和网关校验token，双方使用一样的算法和密钥。这样IM服务生成的token能通过网关的校验。野火已经实现了可以不用关注这一点。
2. 客户端使用自己的网络密钥加密，网关获取到该用户的网络密钥后进行解密。密钥一致就可以正确的Decode数据。野火已经实现了可以不用关注这一点。
3. 网关解密数据是流式的，这样网关可以一边接收数据一边上传数据。也可以全部接收下来后存储为文件再上传，这样就会加大上传的延迟，但实现更为简单。客户可以根据自己的情况来选择那种方案。
4. 实现对第三方对象存储服务的上传，这部分需要客户自己来实现，如上一部分所述，可以一边接收一边上传，也可以接收完成后再上传。
5. 网关在上传成功之后需要给客户端一个响应，响应中有对文件的Key值。客户端使用该Key值和IM服务配置的bucket_domain拼接成最后的链接。

### 下载流程
![oss-gateway_download](http://media.wfcoss.cn/firechat/oss_gateway_download.png)

下载比较简单，客户端直接从第三方存储服务下载文件。链接是网关返回给客户端的Key值和IM服务配置中对应bucket的domain值拼接而成，需要确保该链接能够正确的访问到该文件。

## 配置
IM服务配置:
```
## 媒体服务类型，4是野火对象存储网关
media.server.media_type 4

## 野火对象存储网关的Host，HTTP端口和HTTPS端口。网关需要提供HTTP的端口供移动客户端和PC使用；
## 如果有Web或者小程序客户端，还需要提供HTTPS的端口。
media.server_host  192.168.1.6
media.server_port 8884
media.server_ssl_port 443

## 网关的AK/SK，需要在网关中同样配置
media.access_key tU3vdBK5BL5j4N7jI5N5uZgq_HQDo170w5C9Amnn
media.secret_key YfQIJdgp5YGhwEw14vGpaD2HJZsuJldWtqens7i5

## bucket名字及Domain。至少要区分长期存储的bucket（比如头像收藏等）和可以定期清理的bucket。
## domain为第三方存储服务的文件路径，拼接上网关返回来的key值就可以访问到该文件。
media.bucket_general_name media
media.bucket_general_domain https://oss.thirdpart.com/media
media.bucket_image_name media
media.bucket_image_domain https://oss.thirdpart.com/media
media.bucket_voice_name media
media.bucket_voice_domain https://oss.thirdpart.com/media
media.bucket_video_name media
media.bucket_video_domain https://oss.thirdpart.com/media
media.bucket_file_name media
media.bucket_file_domain https://oss.thirdpart.com/media
media.bucket_sticker_name media
media.bucket_sticker_domain https://oss.thirdpart.com/media
media.bucket_moments_name media
media.bucket_moments_domain https://oss.thirdpart.com/media
media.bucket_portrait_name static
media.bucket_portrait_domain https://oss.thirdpart.com/static
media.bucket_favorite_name static
media.bucket_favorite_domain https://oss.thirdpart.com/static
```

网关服务配置
```
##本地绑定端口。如果还有Web客户端，需要使用nginx提供HTTPS的端口。
local_port 8884

## 数据库类型。0 mysql；1 h2db；2 mysql+mongodb；3 kingbase-v8；4 dameng；5 sql server；6 postgresql。
## 内置数据库不支持网关
embed.db 0

##对象存储AK/SK，需要与IM服务配置保持一致。
media.access_key tU3vdBK5BL5j4N7jI5N5uZgq_HQDo170w5C9Amnn
media.secret_key YfQIJdgp5YGhwEw14vGpaD2HJZsuJldWtqens7i5

##是否使用国密加密，需要确保与IM服务中保持一致。
encrypt.use_sm4 false
```
另外需要配置网关服务的 ```c3p0.xml```，确保网关能够访问IM服务的数据库。


## 对接
检查```UploadFileAction```对象，在此对象中有3个方法如下:
```
//token已经校验，用户密钥已经取到，可以接收数据了
public boolean beforeData();

//接收到的是解密过的数据
public void onData(long pos, byte[] data, int length);

//已经完成所有数据的接收
public void afterData();
```
在这3个方法中野火提供了保存为文件的demo代码。如果客户选择先接收再上传，可以在afterData中把接收到的文件进行上传，并且返回该文件的Key值。也可以边接收边上传，需要删除保存文件的代码，在beforeData做好上传准备，在onData把数据写入上传的流，在afterData中等待上传成功的响应。

对接是客户的责任，在对接之前，如果能够正确保存为文件，就说明野火的处理流程没有问题，后续的对接就需要客户自己来处理来。对接时建议仅修改这3个函数，另外添加部分成员变量。

## IDEA中运行
```io.moquette.server.Server```为服务入口，另外需要配置工作目录为broker目录，这样才可以正确的找到路径进行代码调试。

## 编译
在安装JDK1.8以上及maven的前提下，在命令行中执行```mvn clean package```，生成的目标文件在```./distribution/target/distribution-xxxx-bundle-tar.tar.gz```

## 运行
解压编译后的压缩包，修改config目录下的配置，注意配置要与IM服务的内容保持一致。在bin目录下执行
```
sh ./wildfirehcat.sh
```

## 安全
上传时是经过AES加密的是安全的，下载时也需要提高安全性。可以第三方对象存储服务支持HTTPS下载，IM服务配置中的domain配置为HTTPS地址。另外第三方对象存储服务如果有权限控制机制，也请启动权限控制。
