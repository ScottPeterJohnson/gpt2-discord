FROM gpt-2-submodule
RUN apt-get update && apt-get install -y default-jdk
COPY ./build/install /app
COPY ./bot_samples.py /gpt-2/src
WORKDIR /app/gpt2
CMD ["bin/gpt2"]