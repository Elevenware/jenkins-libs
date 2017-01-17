import com.elevenware.jenkins.pipelines.PipelineContext

def call(String pipelineName, Closure body) {

    PipelineContext context = new PipelineContext()
    body.delegate = context
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body()

    context.dump(System.out)

}