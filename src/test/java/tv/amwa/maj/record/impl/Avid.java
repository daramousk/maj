package tv.amwa.maj.record.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.Test;

import tv.amwa.maj.constant.ContainerConstant;
import tv.amwa.maj.constant.RP224;
import tv.amwa.maj.constant.TransferCharacteristicType;
import tv.amwa.maj.enumeration.ChannelStatusModeType;
import tv.amwa.maj.enumeration.LayoutType;
import tv.amwa.maj.enumeration.ProductReleaseType;
import tv.amwa.maj.exception.AdjacentTransitionException;
import tv.amwa.maj.exception.BadLengthException;
import tv.amwa.maj.exception.BadPropertyException;
import tv.amwa.maj.exception.EventSemanticsException;
import tv.amwa.maj.exception.InsufficientTransitionMaterialException;
import tv.amwa.maj.exception.InvalidDataDefinitionException;
import tv.amwa.maj.exception.LeadingTransitionException;
import tv.amwa.maj.exception.TrackExistsException;
import tv.amwa.maj.extensions.avid.AvidFactory;
import tv.amwa.maj.extensions.avid.CDCIDescriptor;
import tv.amwa.maj.industry.Forge;
import tv.amwa.maj.industry.MediaEngine;
import tv.amwa.maj.industry.PropertyValue;
import tv.amwa.maj.industry.TypeDefinitions;
import tv.amwa.maj.industry.Warehouse;
import tv.amwa.maj.io.aaf.AAFFactory;
import tv.amwa.maj.model.AES3PCMDescriptor;
import tv.amwa.maj.model.ContainerDefinition;
import tv.amwa.maj.model.ContentStorage;
import tv.amwa.maj.model.Identification;
import tv.amwa.maj.model.Locator;
import tv.amwa.maj.model.NetworkLocator;
import tv.amwa.maj.model.Preface;
import tv.amwa.maj.model.Segment;
import tv.amwa.maj.model.Sequence;
import tv.amwa.maj.model.SourceClip;
import tv.amwa.maj.model.SourcePackage;
import tv.amwa.maj.model.TimelineTrack;
import tv.amwa.maj.model.impl.AES3PCMDescriptorImpl;
import tv.amwa.maj.model.impl.CDCIDescriptorImpl;
import tv.amwa.maj.model.impl.ContainerDefinitionImpl;
import tv.amwa.maj.model.impl.DataDefinitionImpl;
import tv.amwa.maj.model.impl.LocatorImpl;
import tv.amwa.maj.model.impl.MaterialPackageImpl;
import tv.amwa.maj.model.impl.NetworkLocatorImpl;
import tv.amwa.maj.model.impl.SegmentImpl;
import tv.amwa.maj.model.impl.SourceClipImpl;
import tv.amwa.maj.model.impl.SourcePackageImpl;
import tv.amwa.maj.model.impl.TaggedValueImpl;
import tv.amwa.maj.model.impl.TapeDescriptorImpl;
import tv.amwa.maj.model.impl.TimecodeSegmentImpl;
import tv.amwa.maj.model.impl.TimelineTrackImpl;
import tv.amwa.maj.record.PackageID;
import tv.amwa.maj.union.impl.SourceReferenceValueImpl;

public class Avid {

    @Test
    public void test() throws IOException, TrackExistsException, NullPointerException, IllegalArgumentException, EventSemanticsException, BadPropertyException, LeadingTransitionException, AdjacentTransitionException, InsufficientTransitionMaterialException {
        MediaEngine.initializeAAF();
        AvidFactory.registerAvidExtensions();
        Preface preface = createAAF();
        AAFFactory.writePreface(preface, "./chronicle-sequence.aaf");
        Path path = Paths.get("./chronicle-sequence.txt");
        Files.write(path, preface.toString().getBytes());
    }

    private static final Preface createAAF() throws InvalidDataDefinitionException, BadLengthException, NullPointerException, EventSemanticsException, BadPropertyException, LeadingTransitionException, AdjacentTransitionException, InsufficientTransitionMaterialException, TrackExistsException {
        String[] types = new String[]{"video", "audio", "audio", "audio", "audio", "audio", "audio", "audio", "audio", "timecode"};
        Identification indentification = Forge.make(
            Identification.class,
            "CompanyName", "Cutting Edge Technical Services",
            "ProductName", "Chronicle", "ProductVersion", new ProductVersionImpl((short)1, (short)0, (short)0, (short)0, ProductReleaseType.Debug)
        );
        // top source mob
        TapeDescriptorImpl topSourceMobEssenceDescriptor = new TapeDescriptorImpl();
        PackageID topSourceMobID = Forge.randomUMID();
        SourcePackageImpl topSourceMob = new SourcePackageImpl(topSourceMobID, "A-Stream_10_00_00_20251023", topSourceMobEssenceDescriptor);
        
        // top master mob
        MaterialPackageImpl masterMob = new MaterialPackageImpl(Forge.randomUMID(), "A-Stream_10_00_00_20251023");
        masterMob.appendPackageUserComment("TapeID", "A-Stream20251023");
        masterMob.appendPackageUserComment("TapeNameLegacy", "A-Stream20251023");
        PropertyValue attrList = TypeDefinitions.TaggedValueStrongReferenceVector.createValue("__AttributeList");
        TaggedValueImpl export = new TaggedValueImpl("_EXPORT", attrList);
        TaggedValueImpl duration = new TaggedValueImpl(
            "Duration", TypeDefinitions.TaggedValueStrongReferenceVector.createValue("00:05:09:13"));
        export.insertTaggedValueAttributeItem(0, duration);
        masterMob.appendMobAttributeItem(export); // TODO add the other ones as well perhaps?     fix locators dynamic    
        ArrayList<tv.amwa.maj.model.Package> packages = new ArrayList<>();
        packages.add(topSourceMob);
        packages.add(masterMob);

        for (int index = 1; index <= types.length; index++) {
            if (types[index].equals("video")) {
                // top source mob (contains all the mobs)
                Segment segment = new SegmentImpl();
                segment.setComponentDataDefinition(DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture));
                segment.setComponentLength(10800000);
                Sequence sequence = segment.generateSequence();
                SourceClip sourceClip = new SourceClipImpl(
                    DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture),
                    10800000, new SourceReferenceValueImpl(Forge.zeroPackageID(), 0, 0l));
                sequence.appendComponentObject(sourceClip);
                
                TimelineTrack track = new TimelineTrackImpl(
                    index + 1, sequence, Forge.makeRational(25, 1), 0);
                    track.setTrackName("");
                track.setEssenceTrackNumber(index);
                topSourceMob.appendPackageTrack(track);

                // master mob
                Segment masterSegment = new SegmentImpl();
                masterSegment.setComponentDataDefinition(DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture));
                masterSegment.setComponentLength(10800000);
                Sequence masterSequence = masterSegment.generateSequence();
                PackageID videoSourceClipPackageID = topSourceMobID;
                SourceClip masterSourceClip = new SourceClipImpl(
                    DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture),
                    7738, new SourceReferenceValueImpl(videoSourceClipPackageID, index, 0l));
                masterSequence.appendComponentObject(masterSourceClip);
                
                TimelineTrack masterTrack = new TimelineTrackImpl(
                    index, masterSequence, Forge.makeRational(25, 1), 0);
                masterTrack.setTrackName("");
                masterTrack.setEssenceTrackNumber(index);
                masterMob.appendPackageTrack(masterTrack);

                // top picture mob
                Segment topPictureMobSegment = new SegmentImpl();
                topPictureMobSegment.setComponentDataDefinition(DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture));
                topPictureMobSegment.setComponentLength(7738);
                Sequence topPictureMobSequence = segment.generateSequence();
                SourceClip topPictureMobSourceClip = new SourceClipImpl(
                    DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture),
                    7738, new SourceReferenceValueImpl(topSourceMobID, index + 2, 0l));
                topPictureMobSequence.appendComponentObject(topPictureMobSourceClip);
                TimelineTrack topPictureMobtrack = new TimelineTrackImpl(
                    index + 1, topPictureMobSequence, Forge.makeRational(25, 1), 0);
                    topPictureMobtrack.setTrackName("");
                topPictureMobtrack.setEssenceTrackNumber(index);
                CDCIDescriptorImpl topPictureMobDescriptor = new CDCIDescriptorImpl();
                NetworkLocatorImpl locator = new NetworkLocatorImpl();
                locator.setURL("file://10.21.6.211/qnap-1/Avid%20MediaFiles/MXF/syd-dailies01.20251023/A_Stream_V012BEE7550.mxf");
                topPictureMobDescriptor.appendLocator(locator);
                topPictureMobDescriptor.setSampleRate(Forge.makeRational(25, 1));
                topPictureMobDescriptor.setEssenceLength(7738l);
                topPictureMobDescriptor.setContainerFormat(Warehouse.lookup(ContainerDefinition.class, ContainerConstant.MXFGC_Clipwrapped_VC3));
                topPictureMobDescriptor.setPictureCompression(RP224.H264_MPEG4_AVC_High_10_Intra_Unconstrained_Coding);
                topPictureMobDescriptor.setStoredHeight(1080);
                topPictureMobDescriptor.setStoredWidth(1920);
                topPictureMobDescriptor.setSampledHeight(1080);
                topPictureMobDescriptor.setSampledWidth(1920);
                topPictureMobDescriptor.setDisplayHeight(1080);
                topPictureMobDescriptor.setDisplayWidth(1920);
                topPictureMobDescriptor.setDisplayXOffset(0);
                topPictureMobDescriptor.setDisplayYOffset(0);
                topPictureMobDescriptor.setFrameLayout(LayoutType.FullFrame);
                topPictureMobDescriptor.setVideoLineMap(new int[]{42, 0});
                topPictureMobDescriptor.setImageAspectRatio(Forge.makeRational(16,9));
                topPictureMobDescriptor.setTransferCharacteristic(TransferCharacteristicType.ITU709);
                topPictureMobDescriptor.setHorizontalSubsampling(2);
                topPictureMobDescriptor.setVerticalSubsampling(1);
                topPictureMobDescriptor.setOffsetToFrameIndexes64(4691066855l);
                topPictureMobDescriptor.setDataOffset(393216);
                topPictureMobDescriptor.setResolutionID(1237);
                // componentwidth and *.box stuff missing?
                SourcePackageImpl topPictureMob = new SourcePackageImpl(videoSourceClipPackageID, null, topPictureMobDescriptor);
                topPictureMob.appendPackageTrack(topPictureMobtrack);
                packages.add(topPictureMob);

            } else if (types[index].equals("audio")) {
                Segment segment = new SegmentImpl();
                segment.setComponentDataDefinition(DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacySound));
                segment.setComponentLength(10800000);
                Sequence sequence = segment.generateSequence();
                SourceClip sourceClip = new SourceClipImpl(
                    DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture),
                    10800000, new SourceReferenceValueImpl(Forge.zeroPackageID(), 0, 0l));
                sequence.appendComponentObject(sourceClip);
                
                TimelineTrack track = new TimelineTrackImpl(
                    index + 1, sequence, Forge.makeRational(25, 1), 0);
                    track.setTrackName("");
                track.setEssenceTrackNumber(index);
                topSourceMob.appendPackageTrack(track);

                // master mob
                Segment masterSegment = new SegmentImpl();
                masterSegment.setComponentDataDefinition(DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacySound));
                masterSegment.setComponentLength(7738);
                Sequence masterSequence = segment.generateSequence();
                PackageID audioSourceClipPackageID = Forge.randomUMID(); // TODO needs to be same with top source mob
                SourceClip masterSourceClip = new SourceClipImpl(
                    DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture),
                    10800000, new SourceReferenceValueImpl(audioSourceClipPackageID, 0, 0l));
                masterSequence.appendComponentObject(masterSourceClip);
                
                TimelineTrack masterTrack = new TimelineTrackImpl(
                    index, masterSequence, Forge.makeRational(25, 1), 0);
                masterTrack.setTrackName("");
                masterTrack.setEssenceTrackNumber(index);
                masterMob.appendPackageTrack(masterTrack);

                // top audio mob
                Segment topAudioSegment = new SegmentImpl();
                topAudioSegment.setComponentDataDefinition(DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacySound));
                //topAudioSegment.setComponentLength(10800000);
                Sequence topAudioSequence = topAudioSegment.generateSequence();
                SourceClip topAudioSourceClip = new SourceClipImpl(
                    DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture),
                    10800000, new SourceReferenceValueImpl(topSourceMobID, 0, 0l));
                topAudioSequence.appendComponentObject(topAudioSourceClip);
                
                TimelineTrack topAudioTrack = new TimelineTrackImpl(
                    index + 1, topAudioSequence, Forge.makeRational(25, 1), 0);
                topAudioTrack.setTrackName("");
                topAudioTrack.setEssenceTrackNumber(index);
                AES3PCMDescriptorImpl topAudioMobDescriptor = new AES3PCMDescriptorImpl();
                NetworkLocatorImpl topAudioLocator = new NetworkLocatorImpl("file://10.21.6.211/qnap-1/Avid%20MediaFiles/MXF/syd-dailies01.20251023/A_Stream_A022BEE9F6F.mxf");
                topAudioMobDescriptor.appendLocator(topAudioLocator);
                topAudioMobDescriptor.setSampleRate(Forge.makeRational(48000, 1));
                topAudioMobDescriptor.setEssenceLength(14856960l);
                topAudioMobDescriptor.setContainerFormat(Warehouse.lookup(ContainerDefinition.class, ContainerConstant.MXFGC_Clipwrapped_AES3_audio_data));
                topAudioMobDescriptor.setQuantizationBits(24);
                topAudioMobDescriptor.setAudioSampleRate(Forge.makeRational(48000, 1));
                topAudioMobDescriptor.setChannelCount(1);
                topAudioMobDescriptor.setAverageBytesPerSecond(144000);
                topAudioMobDescriptor.setBlockAlign((short)3);
                topAudioMobDescriptor.setChannelStatusMode(new ChannelStatusModeType[]{ChannelStatusModeType.Minimum});
                //topAudioMobDescriptor.setFixedChannelStatusData(new byte[]{});
                topAudioMobDescriptor.setDataOffset(393216);
                SourcePackageImpl topSoundMob = new SourcePackageImpl(audioSourceClipPackageID, "", topAudioMobDescriptor);
                topSoundMob.appendPackageTrack(topAudioTrack);
                packages.add(topSoundMob);
            } else if (types[index].equals("timecode")) {
                TimecodeSegmentImpl segment = new TimecodeSegmentImpl();
                segment.setComponentDataDefinition(DataDefinitionImpl.forIdentification(DataDefinitionImpl.Timecode));
                segment.setComponentLength(10800000);
                segment.setStartTimecode(900000);
                segment.setFPS((short)25);
                segment.setDropFrame(false);
                Sequence sequence = segment.generateSequence();
                SourceClip sourceClip = new SourceClipImpl(
                    DataDefinitionImpl.forIdentification(DataDefinitionImpl.LegacyPicture),
                    10800000, new SourceReferenceValueImpl(Forge.zeroPackageID(), 0, 0l));
                sequence.appendComponentObject(sourceClip);
                
                TimelineTrack track = new TimelineTrackImpl(
                    1, sequence, Forge.makeRational(25, 1), 0);
                    track.setTrackName("");
                track.setEssenceTrackNumber(1);
                topSourceMob.appendPackageTrack(track);
            }
        }
        return Forge.make(Preface.class, "ContentStorageObject", Forge.make(
            ContentStorage.class, "Packages",packages.toArray()));
	}
}
