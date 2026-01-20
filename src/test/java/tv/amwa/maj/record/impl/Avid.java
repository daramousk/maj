package tv.amwa.maj.record.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Dictionary;

import org.junit.Test;

import tv.amwa.maj.constant.TransferCharacteristicType;
import tv.amwa.maj.constant.UsageType;
import tv.amwa.maj.enumeration.LayoutType;
import tv.amwa.maj.exception.AdjacentTransitionException;
import tv.amwa.maj.exception.BadPropertyException;
import tv.amwa.maj.exception.EventSemanticsException;
import tv.amwa.maj.exception.InsufficientTransitionMaterialException;
import tv.amwa.maj.exception.LeadingTransitionException;
import tv.amwa.maj.exception.TrackExistsException;
import tv.amwa.maj.extensions.avid.AvidFactory;
import tv.amwa.maj.extensions.avid.CDCIDescriptor;
import tv.amwa.maj.industry.Forge;
import tv.amwa.maj.industry.Warehouse;
import tv.amwa.maj.io.aaf.AAFConstants;
import tv.amwa.maj.io.aaf.AAFFactory;
import tv.amwa.maj.io.mxf.KLVObject;
import tv.amwa.maj.model.AAFFileDescriptor;
import tv.amwa.maj.model.CompositionPackage;
import tv.amwa.maj.model.ContentStorage;
import tv.amwa.maj.model.DataDefinition;
import tv.amwa.maj.model.KLVData;
import tv.amwa.maj.model.Locator;
import tv.amwa.maj.model.MaterialPackage;
import tv.amwa.maj.model.MultipleDescriptor;
import tv.amwa.maj.model.NetworkLocator;
import tv.amwa.maj.model.Preface;
import tv.amwa.maj.model.Sequence;
import tv.amwa.maj.model.SourceClip;
import tv.amwa.maj.model.SourcePackage;
import tv.amwa.maj.model.TapeDescriptor;
import tv.amwa.maj.model.TimelineTrack;
import tv.amwa.maj.model.Track;
import tv.amwa.maj.model.WAVEPCMDescriptor;
import tv.amwa.maj.model.impl.DictionaryImpl;
import tv.amwa.maj.model.impl.KLVDataImpl;
import tv.amwa.maj.record.AUID;

public class Avid {

    @Test
    public void test() throws IOException, TrackExistsException, NullPointerException, IllegalArgumentException, EventSemanticsException, BadPropertyException, LeadingTransitionException, AdjacentTransitionException, InsufficientTransitionMaterialException {
        AvidFactory.registerAvidExtensions();
        Preface preface = makePreface();
        AAFFactory.writePreface(preface, "./chronicle-sequence.aaf");
    }

    public static final Preface makePreface() throws NullPointerException, IllegalArgumentException, TrackExistsException, EventSemanticsException, BadPropertyException, LeadingTransitionException, AdjacentTransitionException, InsufficientTransitionMaterialException {
        int sourceVideoTrackID = 2;
		int sourceAudioTrackID = 3;
        int sourceEssenceLength = 7738;

        TapeDescriptor tapeDescription = Forge.make(TapeDescriptor.class);
        
		SourcePackage sourceTape = Forge.make(SourcePackage.class,
				"PackageID", Forge.randomUMID(), "Name", "A-Stream_10_00_00_20251023",
				"PackageTracks", new Track[] {
                        // TODO the duration is in frames and 30 minutes
						makeTimelineTrack("Picture", null, 0, 0l, 1, sourceEssenceLength), // TODO
						makeTimelineTrack("Sound", null, 0, 0l, 2, sourceEssenceLength) }, // TODO
                "EssenceDescription", tapeDescription);

        SourcePackage sourceFile = Forge.makeAAF("SourceMob", "Name", "A-Stream_10_00_00_20251023",
            "PackageID", Forge.randomUMID(), "PackageTracks", new Track[] {
                makeTimelineTrack("Picture", sourceTape, 1, 0, sourceVideoTrackID, sourceEssenceLength),
                makeTimelineTrack("Sound", sourceTape, 2, 0, sourceAudioTrackID, sourceEssenceLength)
            },
            "EssenceDescription", Forge.make(MultipleDescriptor.class,
            "FileDescriptors", new AAFFileDescriptor[] {
                    makeIMX50VideoDescriptor(sourceEssenceLength, sourceVideoTrackID),
                    makeWAVEPCMDescriptor(sourceEssenceLength, sourceAudioTrackID) },
            "Locators", new Locator[] {
                    Forge.make(NetworkLocator.class, "URL", "file://10.21.6.211/qnap-1/Avid%20MediaFiles/MXF/syd-dailies01.20251023/A_Stream_V012BEE7550.mxf"),
                    Forge.make(NetworkLocator.class, "URL", "file://10.21.6.211/qnap-1/Avid%20MediaFiles/MXF/syd-dailies01.20251023/A_Stream_A012BEE3961.mxf"),
                    // Forge.make(NetworkLocator.class, "URL", ""),
                    // Forge.make(NetworkLocator.class, "URL", ""),
                    // Forge.make(NetworkLocator.class, "URL", ""),
                    // Forge.make(NetworkLocator.class, "URL", ""),
                    // Forge.make(NetworkLocator.class, "URL", ""),
                    // Forge.make(NetworkLocator.class, "URL", ""),
                    // Forge.make(NetworkLocator.class, "URL", "")
                }//,
            )
        );
        
        MaterialPackage masterPackage = Forge.make(
            MaterialPackage.class, "PackageName", "A-Stream_10_00_00_20251023",
            "PackageID", Forge.randomUMID(), "PackageTracks", new Track[]{
                makeTimelineTrack("Picture",
                    sourceFile, sourceVideoTrackID, 0l, sourceVideoTrackID, 125l),
                makeTimelineTrack("Sound", 
                    sourceFile, sourceAudioTrackID, 0l, sourceAudioTrackID, 125) });
        masterPackage.appendPackageUserComment("TapeID", "A-Stream20251023");
        masterPackage.appendPackageUserComment("TapeNameLegacy", "A-Stream20251023");

        // TODO we need duration and start
        //masterPackage.appendPackageUserComment("Start", "10:00:00:00");
        
        CompositionPackage rootComposition = Forge.make(CompositionPackage.class,
                "PackageName", "A-Stream_10_00_00_20251023",
                "PackageID", Forge.randomUMID(),
                "PackageUsage", UsageType.TopLevel);
        Sequence videoSequence = Forge.make(Sequence.class, "ComponentDataDefinition", "Picture");
        Sequence audioSequence = Forge.make(Sequence.class, "ComponentDataDefinition", "Sound");
        videoSequence.appendComponentObject(makeSourceClip("Picture", masterPackage, sourceVideoTrackID, 0, 50));
        audioSequence.appendComponentObject(makeSourceClip("Sound", masterPackage, sourceAudioTrackID, 0, 50));
        rootComposition.appendNewTimelineTrack(
                Forge.makeRational(25, 1), videoSequence, sourceVideoTrackID, "VideoTrack", 0);
        rootComposition.appendNewTimelineTrack(
                Forge.makeRational(25, 1), audioSequence, sourceAudioTrackID, "AudioTrack", 0);
        return Forge.make(Preface.class, "ContentStorageObject", Forge.make(
            ContentStorage.class, "Packages", new tv.amwa.maj.model.Package[]{
                sourceTape, sourceFile, masterPackage
            }));
    }

    public final static CDCIDescriptor makeIMX50VideoDescriptor(
			long essenceLength,
			int linkedTrackID) {

		return Forge.make(CDCIDescriptor.class,
            "SampleRate", "25/1",
            "ContainerFormat", "ContainerDef_MXFGC_Clipwrapped_VC3",
            "EssenceLength", essenceLength,
            "Length", essenceLength,
            "PictureCompression", "urn:smpte:ul:060e2b34.0401010d.04010202.03070100",
            "FrameLayout", LayoutType.FullFrame,
            "VideoLineMap", new int[] { 42, 0 },
            "ImageAspectRatio", Forge.makeRational(16, 9),
            // "AlphaTransparency", AlphaTransparencyType.MinValueTransparent,
            // "ImageAlignmentFactor", 0,
            "TransferCharacteristic", TransferCharacteristicType.ITU709,
            // "ImageStartOffset", 0,
            // "ImageEndOffset", 0,
            // "FieldDominance", FieldNumber.One,
            // "DisplayF2Offset", 0,
            // "StoredF2Offset", 0,
            // "SignalStandard", SignalStandardType.ITU601,
            "DisplayHeight", 1080,
            "DisplayWidth", 1920,
            "DisplayXOffset", 0,
            "DisplayYOffset", 0,
            // "DataOffset", 393216,
            "SampledHeight", 1080,
            "SampledWidth", 1920,
            // "SampledXOffset", 0,
            // "SampledYOffset", 0,
            "StoredHeight", 1080,
            "StoredWidth", 1920,
            // "AlphaSampleDepth", 0,
            // "BlackRefLevel", 16,
            // "ColorRange", 225,
            // "ColorSiting", ColorSitingType.Rec601,
            // "ComponentDepth", 8,
            "HorizontalSubsampling", 2,
            // "PaddingBits", 0,
            // "ReversedByteOrder", false,
            "VerticalSubsampling", 1,
            // "WhiteRefLevel", 235,
            "OffsetToFrameIndexes64", 4691066855L,
            "DataOffset", 393216,
            "ResolutionID", 1237,
            "ComponentWidth", 8,
            // "SourceBox", new Rational[]{Forge.makeRational(-800, 1), Forge.makeRational(-450, 1), Forge.makeRational(1600, 1), Forge.makeRational(900, 1)},
            // "EssenceBox", new Rational[]{Forge.makeRational(-800, 1), Forge.makeRational(-450, 1), Forge.makeRational(1600, 1), Forge.makeRational(900, 1)},
            // "ValidBox", new Rational[]{Forge.makeRational(-800, 1), Forge.makeRational(-450, 1), Forge.makeRational(1600, 1), Forge.makeRational(900, 1)},
            "LinkedTrackID", linkedTrackID
        );
	}

    public final static WAVEPCMDescriptor makeWAVEPCMDescriptor(
        long essenceLength,
        int linledTrackID) {
        return Forge.make(WAVEPCMDescriptor.class,
            "SampleRate", "25/1",
            "ContainerFormat", "ContainerDef_MXFGC_Clipwrapped_AES3_audio_data",
            "EssenceLength", essenceLength,
            "Length", essenceLength,
            "AudioSampleRate", Forge.makeRational(48000, 1),
            "QuantizationBits", 16,
            "ChannelCount", 1,
            "Locked", true,
            "AverageBytesPerSecond", 96000,
            "BlockAlign", 2,
            "ChannelAssignment", "urn:uuid:4b7093c0-c8d2-4f9a-aadc-c1a8d556d3e3",
            "LinkedTrackID", 3);
        }

    public static final TimelineTrack makeTimelineTrack(
        String trackType, tv.amwa.maj.model.Package sourceChainReference, int sourceTrackID, long startPosition,
        int localTrackID, long componentLength) {
            SourceClip clipReference = makeSourceClip(
                trackType, sourceChainReference, sourceTrackID, startPosition, componentLength);
        return Forge.makeAAF("TimelineMobSlot", "SlotID", localTrackID,
            "SlotName", "", "TimelineMobSlotEditRate", "25/1",
            "Origin", 0,
            "MobSlotSegment", clipReference
        );
    }

    public final static SourceClip makeSourceClip(
			String trackType,
			tv.amwa.maj.model.Package sourcePackage,
			int sourceTrackID,
			long startPosition,
			long componentLength) {
		if (sourcePackage != null) {
			return Forge.make(SourceClip.class,
                "ComponentDataDefinition", Warehouse.lookup(DataDefinition.class, trackType),
                "ComponentLength", componentLength,
                "Length", componentLength,
                "SourceTrackID", sourceTrackID,
                "SourcePackageID", sourcePackage.getPackageID(),
                "StartPosition", 0l
            );
		}
		else { // If no source package is provided, make an original source reference
			SourceClip clipReference = Forge.make(SourceClip.class);
			clipReference.setComponentDataDefinition(Warehouse.lookup(DataDefinition.class, trackType));
			clipReference.setComponentLength(componentLength);
			clipReference.setSourceReference(Forge.originalSource());
			return clipReference;
		}
	}
}
