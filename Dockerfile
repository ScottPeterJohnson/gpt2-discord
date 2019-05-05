FROM gpt-2-submodule
RUN apt-get update && apt-get install -y default-jdk
COPY ./build/install /app
WORKDIR /app/gpt2
CMD ["bin/gpt2"]