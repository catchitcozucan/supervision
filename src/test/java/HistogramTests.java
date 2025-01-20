import com.github.catchitcozucan.supervision.api.Histogram;
import com.github.catchitcozucan.supervision.utils.IOUtils;
import com.github.catchitcozucan.supervision.utils.StringUtils;
import com.github.catchitcozucan.supervision.utils.histogram.HistogramCalculi;
import com.github.catchitcozucan.supervision.utils.histogram.HistogramState;
import com.google.gson.Gson;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

import static com.github.catchitcozucan.supervision.utils.IOUtils.resourceToStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HistogramTests {

    private Resource exampleHistogram = new FileSystemResource("exampleStat.json");
    private Gson gson = new Gson();

    @Test
    public void calculiTest() {
        Histogram histogram = jsonResourceToInstance(exampleHistogram, Histogram.class);

        HistogramState state = HistogramCalculi.getState(histogram.getHistogramz()[0].getData(), histogram.getBucketNames());
        assertEquals(3967, state.getInFailState());
        assertEquals("51.7125", Double.valueOf(state.getPercentFinished()).toString());
        assertEquals(8201, state.getSum());
        assertEquals(1321, state.getActuallyFinished());

        Histogram flipped = HistogramCalculi.transForm(histogram, true, false);
        Histogram only = HistogramCalculi.transForm(histogram, false, true);
        Histogram both = HistogramCalculi.transForm(histogram, true, true);
        Histogram none = HistogramCalculi.transForm(histogram, false, false);
        assert (histogram.equals(none));
        assert (Arrays.stream(flipped.getHistogramz()[0].getData()).filter(d -> d < 0).findFirst().isPresent());
        assertFalse(Arrays.stream(only.getHistogramz()[0].getData()).filter(d -> d < 0).findFirst().isPresent());
        assert (Arrays.stream(both.getHistogramz()[0].getData()).filter(d -> d < 0).findFirst().isPresent());
        assertFalse(Arrays.stream(none.getHistogramz()[0].getData()).filter(d -> d < 0).findFirst().isPresent());
        assertEquals(none.getHistogramz()[0].getActualStepProgress(), flipped.getHistogramz()[0].getActualStepProgress());
        assertEquals(flipped.getHistogramz()[0].getActualStepProgress(), only.getHistogramz()[0].getActualStepProgress());
        assertEquals(flipped.getHistogramz()[0].getActualStepProgress(), both.getHistogramz()[0].getActualStepProgress());
    }

    public <T> T jsonResourceToInstance(Resource resource, Class<T> clazz) {
        Optional<InputStream> in = resourceToStream(resource);
        if (in.isPresent()) {
            try {
                return (T) gson.fromJson(StringUtils.fromStreamCloseUponFinish(in.get()), clazz);
            } finally {
                IOUtils.closeQuietly(in.get());
            }
        } else {
            return null;
        }
    }

    /*
    public static Optional<InputStream> resourceToStream(Resource resource) {
        try {
            InputStream in = Thread.currentThread().getContextClassLoader()
                    .getResource(resource.getFilename()).openConnection().getInputStream();
            return Optional.of(in);
        } catch (IOException e) {
            log.error(String.format(String.format("Failed to load %s", resource.getFilename())), e);
        }
        return Optional.empty();
    }
*/
}
