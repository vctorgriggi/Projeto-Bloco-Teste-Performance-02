# syntax=docker/dockerfile:1.7
#
# imagem de a configuracao central. os quatro servicos java usam o mesmo molde; cada um tem o seu
# Dockerfile, no proprio diretorio, porque cada um e construido e implantado sozinho.

# ---- build ---------------------------------------------------------------------------
# o jar e compilado dentro do conteiner, com o wrapper do proprio projeto: a imagem nao
# depende de nada instalado em quem a constroi, e sai igual na maquina de quem
# desenvolve e no pipeline. os testes nao rodam aqui -- rodam no pipeline, antes, com
# relatorio proprio; repeti-los em cada build de imagem so dobraria o tempo.
FROM eclipse-temurin:24-jdk AS build
WORKDIR /build

# primeiro so o que define as dependencias, para a camada de download ficar em cache
# enquanto o pom nao mudar
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
 && cp target/*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extraido

# ---- runtime -------------------------------------------------------------------------
# so o jre, em alpine: a imagem final nao tem compilador, maven nem codigo-fonte.
FROM eclipse-temurin:24-jre-alpine

# o agente do opentelemetry instrumenta http, feign, jdbc, rabbitmq e logback sem uma
# linha de codigo: gera os traces (que atravessam a fila), as metricas e manda os logs
# com o id do trace. versao e checksum fixos -- a imagem nao baixa "a ultima" de nada.
ARG OTEL_AGENT_VERSION=2.16.0
ADD --checksum=sha256:1b0246d3e60b608b07836a9656e1a97bb7d084b088111ef34ecd47483acebcf5 \
    https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/${OTEL_AGENT_VERSION}/opentelemetry-javaagent-${OTEL_AGENT_VERSION}.jar \
    /opt/otel/opentelemetry-javaagent.jar

# processo sem root: se a aplicacao for comprometida, o invasor nao e root no conteiner.
# o uid e numerico e fixo porque o kubernetes, com runAsNonRoot, so aceita a imagem se
# conseguir verificar que o usuario nao e o 0 -- e um nome ("app") ele nao verifica
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app \
 && chmod 644 /opt/otel/opentelemetry-javaagent.jar
WORKDIR /app

# as camadas do spring boot, da que muda menos para a que muda mais. trocar uma linha de
# codigo so invalida a ultima; as dependencias (a maior parte do tamanho) ficam em cache
# no registro e nos nos do cluster.
COPY --from=build /build/extraido/dependencies/ ./
COPY --from=build /build/extraido/spring-boot-loader/ ./
COPY --from=build /build/extraido/snapshot-dependencies/ ./
COPY --from=build /build/extraido/application/ ./

USER 10001
EXPOSE 8888

# MaxRAMPercentage: a jvm dimensiona o heap pelo limite de memoria do conteiner, e nao
# pela memoria da maquina -- sem isso, um limite de 512Mi no kubernetes e uma jvm que
# acha que tem 8Gi terminam em OOMKilled. o agente entra pelo JAVA_TOOL_OPTIONS, e
# OTEL_JAVAAGENT_ENABLED=false o desliga sem trocar de imagem.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -javaagent:/opt/otel/opentelemetry-javaagent.jar" \
    OTEL_SERVICE_NAME=config-server

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
