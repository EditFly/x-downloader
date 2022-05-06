/*
 * JAVE - A Java Audio/Video Encoder (based on FFMPEG)
 * 
 * Copyright (C) 2008-2009 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pukka.iptv.downloader.it.sauronsoftware.jave;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class of the package. Instances can encode audio and video streams.
 * 
 * @author Carlo Pelliccia
 */
public class Encoder {

	/**
	 * This regexp is used to parse the ffmpeg output about the supported
	 * formats.
	 */
	private static final Pattern FORMAT_PATTERN = Pattern
			.compile("^\\s*([D ])([E ])\\s+([\\w,]+)\\s+.+$");

	/**
	 * This regexp is used to parse the ffmpeg output about the included
	 * encoders/decoders.
	 */
	private static final Pattern ENCODER_DECODER_PATTERN = Pattern.compile(
			"^\\s*([D ])([E ])([AVS]).{3}\\s+(.+)$", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the ongoing encoding
	 * process.
	 */
	private static final Pattern PROGRESS_INFO_PATTERN = Pattern.compile(
			"\\s*(\\w+)\\s*=\\s*(\\S+)\\s*", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the size of a video
	 * stream.
	 */
	private static final Pattern SIZE_PATTERN = Pattern.compile(
			"(\\d+)x(\\d+)", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the frame rate value
	 * of a video stream.
	 */
	private static final Pattern FRAME_RATE_PATTERN = Pattern.compile(
			"([\\d.]+)\\s+(?:fps|tb\\(r\\))", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the bit rate value
	 * of a stream.
	 */
	private static final Pattern BIT_RATE_PATTERN = Pattern.compile(
			"(\\d+)\\s+kb/s", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the sampling rate of
	 * an audio stream.
	 */
	private static final Pattern SAMPLING_RATE_PATTERN = Pattern.compile(
			"(\\d+)\\s+Hz", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the channels number
	 * of an audio stream.
	 */
	private static final Pattern CHANNELS_PATTERN = Pattern.compile(
			"(mono|stereo)", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the success of an
	 * encoding operation.
	 */
	private static final Pattern SUCCESS_PATTERN = Pattern.compile(
			"^\\s*video\\:\\S+\\s+audio\\:\\S+\\s+global headers\\:\\S+.*$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * The locator of the ffmpeg executable used by this encoder.
	 */
	private FFMPEGLocator locator;

	/**
	 * It builds an encoder using a {@link DefaultFFMPEGLocator} instance to
	 * locate the ffmpeg executable to use.
	 */
	public Encoder() {
		this.locator = new DefaultFFMPEGLocator();
	}

	/**
	 * It builds an encoder with a custom {@link FFMPEGLocator}.
	 * 
	 * @param locator
	 *            The locator picking up the ffmpeg executable used by the
	 *            encoder.
	 */
	public Encoder(FFMPEGLocator locator) {
		this.locator = locator;
	}

	/**
	 * Returns a list with the names of all the audio decoders bundled with the
	 * ffmpeg distribution in use. An audio stream can be decoded only if a
	 * decoder for its format is available.
	 * 
	 * @return A list with the names of all the included audio decoders.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getAudioDecoders() throws EncoderException {
		ArrayList res = new ArrayList();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		try {
			ffmpeg.execute();
			RBufferedReader reader = null;
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
					if (matcher.matches()) {
						String decoderFlag = matcher.group(1);
						String audioVideoFlag = matcher.group(3);
						if ("D".equals(decoderFlag)
								&& "A".equals(audioVideoFlag)) {
							String name = matcher.group(4);
							res.add(name);
						}
					} else {
						break;
					}
				} else if (line.trim().equals("Codecs:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the audio encoders bundled with the
	 * ffmpeg distribution in use. An audio stream can be encoded using one of
	 * these encoders.
	 * 
	 * @return A list with the names of all the included audio encoders.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getAudioEncoders() throws EncoderException {
		ArrayList res = new ArrayList();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		try {
			ffmpeg.execute();
			RBufferedReader reader = null;
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
					if (matcher.matches()) {
						String encoderFlag = matcher.group(2);
						String audioVideoFlag = matcher.group(3);
						if ("E".equals(encoderFlag)
								&& "A".equals(audioVideoFlag)) {
							String name = matcher.group(4);
							res.add(name);
						}
					} else {
						break;
					}
				} else if (line.trim().equals("Codecs:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the video decoders bundled with the
	 * ffmpeg distribution in use. A video stream can be decoded only if a
	 * decoder for its format is available.
	 * 
	 * @return A list with the names of all the included video decoders.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getVideoDecoders() throws EncoderException {
		ArrayList res = new ArrayList();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		try {
			ffmpeg.execute();
			RBufferedReader reader = null;
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
					if (matcher.matches()) {
						String decoderFlag = matcher.group(1);
						String audioVideoFlag = matcher.group(3);
						if ("D".equals(decoderFlag)
								&& "V".equals(audioVideoFlag)) {
							String name = matcher.group(4);
							res.add(name);
						}
					} else {
						break;
					}
				} else if (line.trim().equals("Codecs:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the video encoders bundled with the
	 * ffmpeg distribution in use. A video stream can be encoded using one of
	 * these encoders.
	 * 
	 * @return A list with the names of all the included video encoders.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getVideoEncoders() throws EncoderException {
		ArrayList res = new ArrayList();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		try {
			ffmpeg.execute();
			RBufferedReader reader = null;
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
					if (matcher.matches()) {
						String encoderFlag = matcher.group(2);
						String audioVideoFlag = matcher.group(3);
						if ("E".equals(encoderFlag)
								&& "V".equals(audioVideoFlag)) {
							String name = matcher.group(4);
							res.add(name);
						}
					} else {
						break;
					}
				} else if (line.trim().equals("Codecs:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the file formats supported at
	 * encoding time by the underlying ffmpeg distribution. A multimedia file
	 * could be encoded and generated only if the specified format is in this
	 * list.
	 * 
	 * @return A list with the names of all the supported file formats at
	 *         encoding time.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getSupportedEncodingFormats() throws EncoderException {
		ArrayList res = new ArrayList();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		try {
			ffmpeg.execute();
			RBufferedReader reader = null;
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = FORMAT_PATTERN.matcher(line);
					if (matcher.matches()) {
						String encoderFlag = matcher.group(2);
						if ("E".equals(encoderFlag)) {
							String aux = matcher.group(3);
							StringTokenizer st = new StringTokenizer(aux, ",");
							while (st.hasMoreTokens()) {
								String token = st.nextToken().trim();
								if (!res.contains(token)) {
									res.add(token);
								}
							}
						}
					} else {
						break;
					}
				} else if (line.trim().equals("File formats:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the file formats supported at
	 * decoding time by the underlying ffmpeg distribution. A multimedia file
	 * could be open and decoded only if its format is in this list.
	 * 
	 * @return A list with the names of all the supported file formats at
	 *         decoding time.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getSupportedDecodingFormats() throws EncoderException {
		ArrayList res = new ArrayList();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		try {
			ffmpeg.execute();
			RBufferedReader reader = null;
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = FORMAT_PATTERN.matcher(line);
					if (matcher.matches()) {
						String decoderFlag = matcher.group(1);
						if ("D".equals(decoderFlag)) {
							String aux = matcher.group(3);
							StringTokenizer st = new StringTokenizer(aux, ",");
							while (st.hasMoreTokens()) {
								String token = st.nextToken().trim();
								if (!res.contains(token)) {
									res.add(token);
								}
							}
						}
					} else {
						break;
					}
				} else if (line.trim().equals("File formats:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a set informations about a multimedia file, if its format is
	 * supported for decoding.
	 * 
	 * @param source
	 *            The source multimedia file.
	 * @return A set of informations about the file and its contents.
	 * @throws InputFormatException
	 *             If the format of the source file cannot be recognized and
	 *             decoded.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public MultimediaInfo getInfo(File source) throws InputFormatException,
			EncoderException {
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-i");
		ffmpeg.addArgument(source.getAbsolutePath());
		try {
			ffmpeg.execute();
		} catch (IOException e) {
			throw new EncoderException(e);
		}
		try {
			RBufferedReader reader = null;
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getErrorStream()));
			return parseMultimediaInfo(source, reader);
		} finally {
			ffmpeg.destroy();
		}
	}

	/**
	 * Private utility. It parses the ffmpeg output, extracting informations
	 * about a source multimedia file.
	 * 
	 * @param source
	 *            The source multimedia file.
	 * @param reader
	 *            The ffmpeg output channel.
	 * @return A set of informations about the source multimedia file and its
	 *         contents.
	 * @throws InputFormatException
	 *             If the format of the source file cannot be recognized and
	 *             decoded.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	private MultimediaInfo parseMultimediaInfo(File source,
			RBufferedReader reader) throws InputFormatException,
			EncoderException {
		Pattern p1 = Pattern.compile("^\\s*Input #0, (\\w+).+$\\s*",
				Pattern.CASE_INSENSITIVE);
		Pattern p2 = Pattern.compile(
				"^\\s*Duration: (\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d).*$",
				Pattern.CASE_INSENSITIVE);
		Pattern p3 = Pattern.compile(
				"^\\s*Stream #\\S+: ((?:Audio)|(?:Video)|(?:Data)): (.*)\\s*$",
				Pattern.CASE_INSENSITIVE);
		MultimediaInfo info = null;
		String absolutePath = source.getAbsolutePath() + ": ";
		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}

				if (line.startsWith(absolutePath)) {
					String message = line.substring(absolutePath.length());
					throw new InputFormatException(message);
				}
				Matcher m1 = p1.matcher(line);
				if (m1.matches()) {
					String format = m1.group(1);
					info = new MultimediaInfo();
					info.setFormat(format);
					continue;
				}

				Matcher m2 = p2.matcher(line);
				if (m2.matches()) {
					long hours = Integer.parseInt(m2.group(1));
					long minutes = Integer.parseInt(m2.group(2));
					long seconds = Integer.parseInt(m2.group(3));
					long dec = Integer.parseInt(m2.group(4));
					long duration = (dec * 100L) + (seconds * 1000L)
							+ (minutes * 60L * 1000L)
							+ (hours * 60L * 60L * 1000L);
					info.setDuration(duration);
				}
				Matcher m3 = p3.matcher(line);
				if (m3.matches()) {
					String type = m3.group(1);
					String specs = m3.group(2);
					if ("Video".equalsIgnoreCase(type)) {
						VideoInfo video = new VideoInfo();
						StringTokenizer st = new StringTokenizer(specs, ",");
						for (int i = 0; st.hasMoreTokens(); i++) {
							String token = st.nextToken().trim();
							if (i == 0) {
								video.setDecoder(token);
							} else {
								// Video size.
								Matcher m31 = SIZE_PATTERN.matcher(token);
								if (m31.find()) {
									int width = Integer.parseInt(m31
											.group(1));
									int height = Integer.parseInt(m31
											.group(2));
									video.setSize(new VideoSize(width,
											height));
								}
								// Frame rate.
								Matcher m32 = FRAME_RATE_PATTERN.matcher(token);
								if (m32.find()) {
									try {
										float frameRate = Float
												.parseFloat(m32.group(1));
										video.setFrameRate(frameRate);
									} catch (NumberFormatException e) {
										e.printStackTrace();
									}
								}
								// Bit rate.
								Matcher m33 = BIT_RATE_PATTERN.matcher(token);
								if (m33.find()) {
									int bitRate = Integer.parseInt(m33
											.group(1));
									video.setBitRate(bitRate);
								}
							}
						}
						info.setVideo(video);
					} else if ("Audio".equalsIgnoreCase(type)) {
						AudioInfo audio = new AudioInfo();
						StringTokenizer st = new StringTokenizer(specs, ",");
						for (int i = 0; st.hasMoreTokens(); i++) {
							String token = st.nextToken().trim();
							if (i == 0) {
								audio.setDecoder(token);
							} else {
								// Sampling rate.
								Matcher m31 = SAMPLING_RATE_PATTERN
										.matcher(token);
								if (m31.find()) {
									int samplingRate = Integer.parseInt(m31
											.group(1));
									audio.setSamplingRate(samplingRate);
								}
								// Channels.
								Matcher m32 = CHANNELS_PATTERN.matcher(token);
								if (m32.find()) {
									String ms = m32.group(1);
									if ("mono".equalsIgnoreCase(ms)) {
										audio.setChannels(1);
									} else if ("stereo"
											.equalsIgnoreCase(ms)) {
										audio.setChannels(2);
									}
								}
								// Bit rate.
								Matcher m33 = BIT_RATE_PATTERN.matcher(token);
								if (m33.find()) {
									int bitRate = Integer.parseInt(m33
											.group(1));
									audio.setBitRate(bitRate);
								}
							}
						}
						info.setAudio(audio);
					}
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		}
		if (info == null) {
			throw new InputFormatException();
		}
		return info;
	}

	/**
	 * Private utility. Parse a line and try to match its contents against the
	 * {@link Encoder#PROGRESS_INFO_PATTERN} pattern. It the line can be parsed,
	 * it returns a hashtable with progress informations, otherwise it returns
	 * null.
	 * 
	 * @param line
	 *            The line from the ffmpeg output.
	 * @return A hashtable with the value reported in the line, or null if the
	 *         given line can not be parsed.
	 */
	private Hashtable parseProgressInfoLine(String line) {
		Hashtable table = null;
		Matcher m = PROGRESS_INFO_PATTERN.matcher(line);
		while (m.find()) {
			if (table == null) {
				table = new Hashtable();
			}
			String key = m.group(1);
			String value = m.group(2);
			table.put(key, value);
		}
		return table;
	}

	/**
	 * Re-encode a multimedia file.
	 * 
	 * @param source
	 *            The source multimedia file. It cannot be null. Be sure this
	 *            file can be decoded (see
	 *            {@link Encoder#getSupportedDecodingFormats()},
	 *            {@link Encoder#getAudioDecoders()} and
	 *            {@link Encoder#getVideoDecoders()}).
	 * @param target
	 *            The target multimedia re-encoded file. It cannot be null. If
	 *            this file already exists, it will be overwrited.
	 * @param attributes
	 *            A set of attributes for the encoding process.
	 * @throws IllegalArgumentException
	 *             If both audio and video parameters are null.
	 * @throws InputFormatException
	 *             If the source multimedia file cannot be decoded.
	 * @throws EncoderException
	 *             If a problems occurs during the encoding process.
	 */
	public void encode(File source, File target, EncodingAttributes attributes)
			throws IllegalArgumentException, InputFormatException,
			EncoderException {
		encode(source, target, attributes, null);
	}

	/**
	 * Re-encode a multimedia file.
	 * 
	 * @param source
	 *            The source multimedia file. It cannot be null. Be sure this
	 *            file can be decoded (see
	 *            {@link Encoder#getSupportedDecodingFormats()},
	 *            {@link Encoder#getAudioDecoders()} and
	 *            {@link Encoder#getVideoDecoders()}).
	 * @param target
	 *            The target multimedia re-encoded file. It cannot be null. If
	 *            this file already exists, it will be overwrited.
	 * @param attributes
	 *            A set of attributes for the encoding process.
	 * @param listener
	 *            An optional progress listener for the encoding process. It can
	 *            be null.
	 * @throws IllegalArgumentException
	 *             If both audio and video parameters are null.
	 * @throws InputFormatException
	 *             If the source multimedia file cannot be decoded.
	 * @throws EncoderException
	 *             If a problems occurs during the encoding process.
	 */
	public void encode(File source, File target, EncodingAttributes attributes,
			EncoderProgressListener listener) throws IllegalArgumentException,
			InputFormatException, EncoderException {
		String formatAttribute = attributes.getFormat();
		Float offsetAttribute = attributes.getOffset();
		Float durationAttribute = attributes.getDuration();
		AudioAttributes audioAttributes = attributes.getAudioAttributes();
		VideoAttributes videoAttributes = attributes.getVideoAttributes();
		if (audioAttributes == null && videoAttributes == null) {
			throw new IllegalArgumentException(
					"Both audio and video attributes are null");
		}
		target = target.getAbsoluteFile();
		target.getParentFile().mkdirs();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		if (offsetAttribute != null) {
			ffmpeg.addArgument("-ss");
			ffmpeg.addArgument(String.valueOf(offsetAttribute.floatValue()));
		}
		ffmpeg.addArgument("-i");
		ffmpeg.addArgument(source.getAbsolutePath());
		if (durationAttribute != null) {
			ffmpeg.addArgument("-t");
			ffmpeg.addArgument(String.valueOf(durationAttribute.floatValue()));
		}
		if (videoAttributes == null) {
			ffmpeg.addArgument("-vn");
		} else {
			String codec = videoAttributes.getCodec();
			if (codec != null) {
				ffmpeg.addArgument("-vcodec");
				ffmpeg.addArgument(codec);
			}
			String tag = videoAttributes.getTag();
			if (tag != null) {
				ffmpeg.addArgument("-vtag");
				ffmpeg.addArgument(tag);
			}
			Integer bitRate = videoAttributes.getBitRate();
			if (bitRate != null) {
				ffmpeg.addArgument("-b");
				ffmpeg.addArgument(String.valueOf(bitRate.intValue()));
			}
			Integer frameRate = videoAttributes.getFrameRate();
			if (frameRate != null) {
				ffmpeg.addArgument("-r");
				ffmpeg.addArgument(String.valueOf(frameRate.intValue()));
			}
			VideoSize size = videoAttributes.getSize();
			if (size != null) {
				ffmpeg.addArgument("-s");
				ffmpeg.addArgument(String.valueOf(size.getWidth()) + "x"
						+ String.valueOf(size.getHeight()));
			}
		}
		if (audioAttributes == null) {
			ffmpeg.addArgument("-an");
		} else {
			String codec = audioAttributes.getCodec();
			if (codec != null) {
				ffmpeg.addArgument("-acodec");
				ffmpeg.addArgument(codec);
			}
			Integer bitRate = audioAttributes.getBitRate();
			if (bitRate != null) {
				ffmpeg.addArgument("-ab");
				ffmpeg.addArgument(String.valueOf(bitRate.intValue()));
			}
			Integer channels = audioAttributes.getChannels();
			if (channels != null) {
				ffmpeg.addArgument("-ac");
				ffmpeg.addArgument(String.valueOf(channels.intValue()));
			}
			Integer samplingRate = audioAttributes.getSamplingRate();
			if (samplingRate != null) {
				ffmpeg.addArgument("-ar");
				ffmpeg.addArgument(String.valueOf(samplingRate.intValue()));
			}
			Integer volume = audioAttributes.getVolume();
			if (volume != null) {
				ffmpeg.addArgument("-vol");
				ffmpeg.addArgument(String.valueOf(volume.intValue()));
			}
		}
		ffmpeg.addArgument("-f");
		ffmpeg.addArgument(formatAttribute);
		ffmpeg.addArgument("-y");
		ffmpeg.addArgument(target.getAbsolutePath());
		try {
			ffmpeg.execute();
		} catch (IOException e) {
			throw new EncoderException(e);
		}
		try {
			RBufferedReader reader = new RBufferedReader(new InputStreamReader(ffmpeg.getErrorStream()));
			processErrorOutput(attributes, reader, source, listener);
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
			ffmpeg.destroy();
		}
	}


	protected void processErrorOutput(EncodingAttributes attributes, BufferedReader errorReader, File source, EncoderProgressListener listener) throws EncoderException, IOException {
		String lastWarning = null;
		long progress = 0L;
		Float offsetAttribute = attributes.getOffset();
		MultimediaInfo info = parseMultimediaInfo(source, (RBufferedReader)errorReader);
		Float durationAttribute = attributes.getDuration();
		long duration;
		if (durationAttribute != null) {
			duration = Math.round(durationAttribute.floatValue() * 1000.0F);
		}
		else {
			duration = info.getDuration();
			if (offsetAttribute != null) {
				duration -= Math.round(offsetAttribute.floatValue() * 1000.0F);
			}
		}

		if (listener != null) {
			listener.sourceInfo(info);
		}
		int step = 0;
		String line;
		while ((line = errorReader.readLine()) != null) {
			if (step == 0) {
				if (line.startsWith("WARNING: ")) {
					if (listener != null)
						listener.message(line);
				} else {
					if (!line.startsWith("Output #0")) {
						throw new EncoderException(line);
					}
					step++;
				}
			} else if ((step == 1) &&
					(!line.startsWith("  "))) {
				step++;
			}

			if (step == 2) {
				if (!line.startsWith("Stream mapping:")) {
					throw new EncoderException(line);
				}
				step++;
			}
			else if ((step == 3) &&
					(!line.startsWith("  "))) {
				step++;
			}

			if (step == 4) {
				line = line.trim();
				if (line.length() > 0) {
					Hashtable table = parseProgressInfoLine(line);
					if (table == null) {
						if (listener != null) {
							listener.message(line);
						}
						lastWarning = line;
					} else {
						if (listener != null) {
							String time = (String)table.get("time");
							if (time != null) {
								int dot = time.indexOf(46);
								if ((dot > 0) && (dot == time.length() - 2) && (duration > 0L))
								{
									String p1 = time.substring(0, dot);
									String p2 = time.substring(dot + 1);
									try {
										long i1 = Long.parseLong(p1);
										long i2 = Long.parseLong(p2);
										progress = i1 * 1000L + i2 * 100L;

										int perm = (int)Math.round(progress * 1000L / duration);

										if (perm > 1000) {
											perm = 1000;
										}
										listener.progress(perm);
									}
									catch (NumberFormatException e) {
									}
								}
							}
						}
						lastWarning = null;
					}
				}
			}
		}
		if ((lastWarning != null) &&
				(!SUCCESS_PATTERN.matcher(lastWarning).matches()))
			throw new EncoderException(lastWarning);
	}

}