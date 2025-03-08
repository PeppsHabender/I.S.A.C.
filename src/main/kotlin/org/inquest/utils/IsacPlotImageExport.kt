package org.inquest.utils

import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import org.jetbrains.kotlinx.kandy.letsplot.export.toSVG
import org.jetbrains.kotlinx.kandy.letsplot.multiplot.model.PlotGrid
import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.core.util.PlotSvgHelper.fetchPlotSizeFromSvg
import java.io.ByteArrayOutputStream
import java.io.StringReader

/**
 * Helper function to allow isac to create grid plots with a custom background color.
 */
fun PlotGrid.toIsacPNG(bg: Color = Color(21, 21, 21)): ByteArray {
    val svg = toSVG().replace("rgb(255,255,255)", "rgb(${bg.red},${bg.green},${bg.blue})")
    val plotSize = fetchPlotSizeFromSvg(svg)

    val transcoder = PNGTranscoder().apply {
        addTranscodingHint(ImageTranscoder.KEY_WIDTH, plotSize.x.toFloat())
        addTranscodingHint(ImageTranscoder.KEY_HEIGHT, plotSize.y.toFloat())
    }

    return ByteArrayOutputStream().also { transcoder.transcode(TranscoderInput(StringReader(svg)), TranscoderOutput(it)) }.toByteArray()
}
